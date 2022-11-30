package com.browserstack.automate.ci.jenkins.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit; 
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import hudson.model.Queue;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.util.TimeDuration;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.interceptor.RequirePOST;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;

@ExportedBean
public class BuildWithObservabilityConfigAction<
        JobT extends Job<JobT, RunT> & ParameterizedJobMixIn.ParameterizedJob,
        RunT extends Run<JobT, RunT> & Queue.Executable>
        implements Action {

    /** The form parameter that holds the Eiffel links. */
    public static final String FORM_PARAM_OBSERVABILITY = "testopsParams";

    /** The form parameter that holds the build parameters. */
    public static final String FORM_PARAM_PARAMETERS = "parameter";

    /**
     * The immediate suffix to the URL of the {@link Job}, i.e. the URLs served by this action will have
     * the form $JOB_URL/$URL_SUFFIX.
     */
    public static final String URL_SUFFIX = "testops";

    private final JobT job;
    private String params;

    public BuildWithObservabilityConfigAction(JobT job) {
        this.job = job;
    }

    /**
     * Responds to a /build request by parsing the posted form and transforming the Eiffel links
     * and (optionally) build parameters into actions that'll get passed to the new build.
     */
    @RequirePOST
    public void doBuild(StaplerRequest req, StaplerResponse rsp, @QueryParameter TimeDuration delay)
            throws IOException {
        job.checkPermission(Item.BUILD);

        if (!job.isBuildable()) {
            throw HttpResponses.error(SC_CONFLICT, new IOException(job.getFullName() + " is not buildable"));
        }

        if (delay == null) {
            delay = new TimeDuration(TimeUnit.MILLISECONDS.convert(job.getQuietPeriod(), TimeUnit.SECONDS));
        }

        List<Action> actions = new ArrayList<>();
        JSONObject formData;
        try {
            formData = req.getSubmittedForm();
            params = formData.toString();
        }
        catch (ServletException e) {
            throw HttpResponses.error(SC_BAD_REQUEST,
                    new IllegalArgumentException(
                            "Missing or invalid contents of \"json\" form field: " + e.toString(), e));
        }
        try {
            ParametersDefinitionProperty pp = job.getProperty(ParametersDefinitionProperty.class);
            if (pp != null) {
                Action paramAction = getParametersAction(req, formData, pp, FORM_PARAM_PARAMETERS);
                if (paramAction != null) {
                    actions.add(paramAction);
                }
            }

            List<Cause> causes = new ArrayList<>(Arrays.asList(getCallerCause(req)));
            ObservabilityCause observabilityCause = getObservabilityCause(formData);
            if (observabilityCause != null) {
                causes.add(observabilityCause);
            }
            actions.add(new CauseAction(causes));
        }
        catch (IllegalArgumentException e) {
            throw HttpResponses.error(SC_BAD_REQUEST, e);
        }

        Queue.Item queuedBuild = Jenkins.getInstance().getQueue().schedule2(job, 0, actions).getItem();
        if (queuedBuild != null) {
            rsp.sendRedirect(SC_CREATED, req.getContextPath() + '/' + queuedBuild.getUrl());
        } else {
            rsp.sendRedirect(".");
        }
    }

    /**
     * Parses the posted form and transforms the supplied list of Observability params
     * into an {@link ObservabilityCause}. The JSON object in the form value must have a
     * {@link #FORM_PARAM_OBSERVABILITY} key that contains the array of Observability params.
     * The array may be empty in which case null is returned.
     */
    @CheckForNull
    private ObservabilityCause getObservabilityCause(final JSONObject formData) {
        // This mixing of different parsed JSON representation isn't great, but the StaplerRequest
        // provides us a JSONObject and we want to use Jackson for the events themselves.
        try {
            JSONObject observabilityParams = formData.getJSONObject(FORM_PARAM_OBSERVABILITY);
            return observabilityParams.isEmpty() ? null : new ObservabilityCause(observabilityParams);
        }
        catch (JSONException e) {
            throw new IllegalArgumentException(String.format(
                    "URL parameter '%s' did not contain a JSON array", FORM_PARAM_OBSERVABILITY), e);
        }
    }

    /**
     * Returns a {@link Cause} that indicates the caller that requested the build (either a
     * {@link Cause.RemoteCause} or a {@link Cause.UserIdCause}).
     * <p>
     * The implementation was copied from
     * {@link ParameterizedJobMixIn#getBuildCause(ParameterizedJobMixIn.ParameterizedJob, StaplerRequest)}
     * and only slightly modified.
     */
    public Cause getCallerCause(StaplerRequest req) {
        @SuppressWarnings("deprecation")
        hudson.model.BuildAuthorizationToken authToken = job.getAuthToken();
        if (authToken != null && authToken.getToken() != null && req.getParameter("token") != null) {
            // Optional additional cause text when starting via token
            String causeText = req.getParameter("cause");
            return new Cause.RemoteCause(req.getRemoteAddr(), causeText);
        } else {
            return new Cause.UserIdCause();
        }
    }

    /**
     * Attempts to parse the posted form and transform the given build parameters and their
     * values into a {@link ParametersAction}. The JSON object in the form value may have a
     * {@link #FORM_PARAM_PARAMETERS} key that contains another object with the desired
     * build parameters. If the {@link #FORM_PARAM_PARAMETERS} key is missing the default
     * values of the parameters will be used. Returns null if no parameters were supplied.
     */
    @CheckForNull
    private ParametersAction getParametersAction(final StaplerRequest req,
                                                 final JSONObject formData,
                                                 final ParametersDefinitionProperty pp,
                                                 final String formParam) {
        List<ParameterValue> values = new ArrayList<>();

        // Collect parameters given in this request.
        List<JSONObject> givenParams = new ArrayList<>();
        try {
            Object inputParams = formData.get(formParam);
            if (inputParams != null) {
                for (Object paramObject : JSONArray.fromObject(inputParams)) {
                    givenParams.add((JSONObject) paramObject);
                }
            }
        }
        catch (ClassCastException | JSONException e) {
            throw new IllegalArgumentException(String.format(
                    "URL parameter '%s' couldn't be deserialized to a parameter list: %s",
                    formParam, e.toString()), e);
        }

        // Did the request include any parameters that haven't been defined for this job?
        Set<String> missingParamDefs = givenParams.stream()
                .map(p -> p.getString("name"))
                .filter(n -> pp.getParameterDefinition(n) == null)
                .collect(Collectors.toSet());
        if (!missingParamDefs.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Build request provided values for the following parameters that aren't defined in the job: %s'",
                    missingParamDefs));
        }

        // Add parameter values for all given parameters.
        for (JSONObject param : givenParams) {
            String name = param.getString("name");
            ParameterValue parameterValue = pp.getParameterDefinition(name).createValue(req, param);
            if (parameterValue != null) {
                values.add(parameterValue);
            } else {
                throw new IllegalArgumentException(String.format(
                        "Cannot initialize the '%s' parameter with the given value", name));
            }
        }

        // Add default parameter values for parameters that haven't been given values above.
        Set<String> paramsWithValues = values.stream()
                .map(ParameterValue::getName)
                .collect(Collectors.toSet());
        values.addAll(pp.getParameterDefinitions().stream()
                .filter(pd -> !paramsWithValues.contains(pd.getName()))
                .map(ParameterDefinition::getDefaultParameterValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        return values.isEmpty() ? null : new ParametersAction(values);
    }

    @Exported
    public String getParams() {
        return params;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return URL_SUFFIX;
    }
}
