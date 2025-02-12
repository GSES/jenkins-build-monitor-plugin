package com.smartcodeltd.jenkinsci.plugins.buildmonitor.viewmodel;

import com.smartcodeltd.jenkinsci.plugins.buildmonitor.viewmodel.duration.Duration;
import hudson.model.Result;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

public interface BuildViewModel {
    public String name();
    public String url();
    public Result result();

    public boolean isRunning();
    public Duration elapsedTime();
    public Duration timeElapsedSince();
    public Duration duration();
    public Duration estimatedDuration();
	public Calendar timestamp();
    public int progress();

    public boolean hasPreviousBuild();
    public BuildViewModel previousBuild();

    public Set<String> culprits();

    boolean isClaimed();
    String claimant();
    String reasonForClaim();

    boolean hasKnownFailures();

    List<String> knownFailures();
}
