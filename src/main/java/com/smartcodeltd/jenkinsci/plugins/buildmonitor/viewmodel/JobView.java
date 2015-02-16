package com.smartcodeltd.jenkinsci.plugins.buildmonitor.viewmodel;

import com.smartcodeltd.jenkinsci.plugins.buildmonitor.facade.RelativeLocation;
import com.smartcodeltd.jenkinsci.plugins.buildmonitor.viewmodel.duration.Duration;
import com.smartcodeltd.jenkinsci.plugins.buildmonitor.viewmodel.plugins.BuildAugmentor;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.*;

import static hudson.model.Result.*;

/**
 * @author Jan Molak
 */
public class JobView {
    private final Date systemTime;
    private final Job<?, ?> job;
    private final BuildAugmentor augmentor;
    private final RelativeLocation relative;

    private final static Map<Result, String> statuses = new HashMap<Result, String>() {{
        put(SUCCESS,   "successful");
        put(UNSTABLE,  "unstable");
        put(FAILURE,   "failing");
        put(ABORTED,   "failing");  // if someone has aborted it then something is clearly not right, right? :)
    }};

    public static JobView of(Job<?, ?> job) {
        return new JobView(job, new BuildAugmentor(), RelativeLocation.of(job), new Date());
    }

    public static JobView of(Job<?, ?> job, BuildAugmentor augmentor) {
        return new JobView(job, augmentor, RelativeLocation.of(job), new Date());
    }

    public static JobView of(Job<?, ?> job, RelativeLocation location) {
        return new JobView(job, new BuildAugmentor(), location, new Date());
    }

    public static JobView of(Job<?, ?> job, Date systemTime) {
        return new JobView(job, new BuildAugmentor(), RelativeLocation.of(job), systemTime);
    }

    @JsonProperty
    public String name() {
        return relative.name();
    }

    @JsonProperty
    public String url() {
        return relative.url();
    }

    private String statusOf(Result result) {
        return statuses.containsKey(result) ? statuses.get(result) : "unknown";
    }

    @JsonProperty
    public String status() {
        String status = statusOf(lastCompletedBuild().result());

        if (lastBuild().isRunning()) {
            status += " running";
        }

        if (lastCompletedBuild().isClaimed()) {
            status += " claimed";
        }

        return status;
    }

    @JsonProperty
    public String lastBuildName() {
        return lastBuild().name();
    }

    @JsonProperty
    public String lastBuildUrl() {
        return lastBuild().url();
    }

    @JsonProperty
    public String lastBuildDuration() {
        if (lastBuild().isRunning()) {
            return formatted(lastBuild().elapsedTime());
        }

        return formatted(lastBuild().duration());
    }

    @JsonProperty
    public String lastBuildTime() {
		long millis = System.currentTimeMillis() - lastBuild().timestamp().getTime().getTime();
		long minutes = millis / 1000 / 60;

		String formatted;
		if (minutes > 10080) {
			long weeks = minutes / 60 / 24 / 7;
			formatted = weeks == 1 ? "1 week ago" : weeks + " weeks ago";
		} else if (minutes > 1440) {
			long days = minutes / 60 / 24;
			formatted = days == 1 ? "1 day ago" : days + " days ago";
		} else if (minutes > 60) {
			long hours = minutes / 60;
			formatted = hours == 1 ? "1 hour ago" : hours + " hours ago";
		} else if (minutes == 0) {
			formatted = "just now";
		} else {
			formatted = minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
		}

        return formatted;
    }

    @JsonProperty
    public String estimatedDuration() {
        return formatted(lastBuild().estimatedDuration());
    }

    @JsonProperty
    public String timeElapsedSinceLastBuild() {
        return formatted(lastCompletedBuild().timeElapsedSince());
    }

    private String formatted(Duration duration) {
        return null != duration
                ? duration.toString()
                : "";
    }

    @JsonProperty
    public int progress() {
        return lastBuild().progress();
    }

    @JsonProperty
    public boolean shouldIndicateCulprits() {
        return ! isClaimed() && culprits().size() > 0;
    }

    @JsonProperty
    public Set<String> culprits() {
        Set<String> culprits = new HashSet<String>();

        BuildViewModel build = lastBuild();
        // todo: consider introducing a BuildResultJudge to keep this logic in one place
        while (! SUCCESS.equals(build.result())) {
            culprits.addAll(build.culprits());

            if (! build.hasPreviousBuild()) {
                break;
            }

            build = build.previousBuild();
        };

        return culprits;
    }

    @JsonProperty
    public boolean isClaimed() {
        return lastCompletedBuild().isClaimed();
    }

    @JsonProperty
    public String claimAuthor() {
        return lastCompletedBuild().claimant();
    }

    @JsonProperty
    public String claimReason() {
        return lastCompletedBuild().reasonForClaim();
    }

    @JsonProperty
    public boolean hasKnownFailures() {
        return lastCompletedBuild().hasKnownFailures();
    }

    @JsonProperty
    public List<String> knownFailures() {
        return lastCompletedBuild().knownFailures();
    }

    public String toString() {
        return name();
    }


    private JobView(Job<?, ?> job, BuildAugmentor augmentor, RelativeLocation relative, Date systemTime) {
        this.job        = job;
        this.augmentor  = augmentor;
        this.systemTime = systemTime;
        this.relative   = relative;
    }

    private BuildViewModel lastBuild() {
        return buildViewOf(job.getLastBuild());
    }

    private BuildViewModel lastCompletedBuild() {
        BuildViewModel previousBuild = lastBuild();
        while (previousBuild.isRunning() && previousBuild.hasPreviousBuild()) {
            previousBuild = previousBuild.previousBuild();
        }

        return previousBuild;
    }

    private BuildViewModel buildViewOf(Run<?, ?> build) {
        if (null == build) {
            return new NullBuildView();
        }

        return BuildView.of(job.getLastBuild(), augmentor, relative, systemTime);
    }
}
