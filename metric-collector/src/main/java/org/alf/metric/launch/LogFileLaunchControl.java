package org.alf.metric.launch;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.afc.parse.CapturingGroupNamedValueParser;
import org.afc.parse.NamedValueParser;
import org.alf.metric.buffer.BufferListener;
import org.alf.metric.collector.FileCollector;
import org.alf.metric.config.Config.Capture;
import org.alf.metric.config.Config.Launch;
import org.alf.metric.config.Config.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class LogFileLaunchControl implements LaunchControl, BufferListener {

	private static final Logger logger = LoggerFactory.getLogger(LogFileLaunchControl.class);

	private boolean launched;

	private long adjustment;

	private Launch launch;

	private Capture capture;

	private FileCollector<Source> collector;

	private NamedValueParser parser;

	private DateTimeFormatter formatter;

	private boolean stopped;

	public LogFileLaunchControl(Launch launch, Capture capture, FileCollector<Source> collector) {
		this.launched = false;
		this.stopped = false;
		this.launch = launch;
		this.capture = capture;
		this.collector = collector;
		this.parser = new CapturingGroupNamedValueParser(capture.getPattern());
		this.collector.addBufferListener(this);
		if (capture.getTimestamp() != null && capture.getTimestamp().getPattern() != null) {
			this.formatter = DateTimeFormatter.ofPattern(capture.getTimestamp().getPattern());
		}
	}

	@Override
	public void onReceived(CharSequence recvBuffer) {
		if (!capture.getContains().stream().allMatch(recvBuffer.toString()::contains)) {
			return;
		}
		Map<String, String> entries = parser.parse(recvBuffer);
		if (entries.isEmpty()) {
			return;
		}
		logger.info("launch pattern found, going to launch all collectors");
		//Time
		if (launch.getGenesis() != null && formatter != null && capture.getTimestamp().getTag() != null) {
			OffsetDateTime markerTime = OffsetDateTime.parse(entries.get(capture.getTimestamp().getTag()), formatter);
			adjustment = launch.getGenesis().toInstant().toEpochMilli() - markerTime.toInstant().toEpochMilli();
			logger.info("genesis adjustment:[{}]", adjustment);
		}

		launched = true;
		stop();
	}

	@Override
	public void stop() {
		if (!stopped) {
			stopped = true;
			collector.stop();
			logger.info("launch control stopped");
		}
	}
}
