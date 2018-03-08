package org.alf.metric.launch;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NoLaunchControl implements LaunchControl {

	@Override
	public boolean isLaunched() {
		return true;
	}

	@Override
	public long getAdjustment() {
		return 0;
	}
}
