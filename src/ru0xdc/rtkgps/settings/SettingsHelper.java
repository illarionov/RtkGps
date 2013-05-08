package ru0xdc.rtkgps.settings;

import javax.annotation.Nonnull;

import ru0xdc.rtklib.ProcessingOptions;
import ru0xdc.rtklib.RtkServerSettings;
import ru0xdc.rtklib.RtkServerSettings.InputStream;
import ru0xdc.rtklib.RtkServerSettings.LogStream;
import ru0xdc.rtklib.RtkServerSettings.OutputStream;
import ru0xdc.rtklib.SolutionOptions;
import ru0xdc.rtklib.constants.SolutionFormat;
import ru0xdc.rtklib.constants.StreamFormat;
import ru0xdc.rtklib.constants.StreamType;
import android.content.Context;
import android.content.SharedPreferences;

public class SettingsHelper {

	public static void setDefaultValues(Context ctx, boolean force) {

		// XXX SuolutionOutputFragment

		ProcessingOptions1Fragment.setDefaultValues(ctx, force);
		InputRoverFragment.setDefaultValues(ctx, force);
		InputBaseFragment.setDefaultValues(ctx, force);
		InputCorrectionFragment.setDefaultValues(ctx, force);
		OutputSolution1Fragment.setDefaultValues(ctx, force);
		OutputSolution2Fragment.setDefaultValues(ctx, force);
		LogRoverFragment.setDefaultValues(ctx, force);
		LogBaseFragment.setDefaultValues(ctx, force);
		LogCorrectionFragment.setDefaultValues(ctx, force);
	}


	public static RtkServerSettings loadSettings(Context ctx) {
		final RtkServerSettings settings;
		ProcessingOptions procOpts;
		SolutionOptions solOptsBase;

		settings = new RtkServerSettings();

		procOpts = ProcessingOptions1Fragment.readPrefs(ctx);
		settings.setProcessingOptions(procOpts);

		solOptsBase = SolutionOutputSettingsFragment.readPrefs(ctx);

		settings
			.setInputRover(InputRoverFragment.readPrefs(ctx))
			.setInputBase(InputBaseFragment.readPrefs(ctx))
			.setInputCorrection(InputCorrectionFragment.readPrefs(ctx))
			.setOutputSolution1(OutputSolution1Fragment.readPrefs(ctx, solOptsBase))
			.setOutputSolution2(OutputSolution2Fragment.readPrefs(ctx, solOptsBase))
			.setLogRover(LogRoverFragment.readPrefs(ctx))
			.setLogBase(LogBaseFragment.readPrefs(ctx))
			.setLogCorrection(LogCorrectionFragment.readPrefs(ctx))
			;

		// TODO: send NMEA to base setting

		return settings;
	}

	static void setInputStreamDefaultValues(Context ctx, String sharedPrefsName, boolean force) {
		final SharedPreferences prefs;

		prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

		final boolean needUpdate = force || !prefs.contains(InputRoverFragment.KEY_ENABLE);

		if (needUpdate) {
			SharedPreferences.Editor e = prefs.edit();
			e.putBoolean(InputRoverFragment.KEY_ENABLE, false)
			 .putString(InputRoverFragment.KEY_TYPE, StreamType.NTRIPCLI.name())
			 .putString(InputRoverFragment.KEY_FORMAT, StreamFormat.RTCM3.name())
			 .putString(InputRoverFragment.KEY_COMMANDS_AT_STARTUP, "")
			 .putString(InputRoverFragment.KEY_COMMANDS_AT_SHUTDOWN, "")
			 .putString(InputRoverFragment.KEY_RECEIVER_OPTION, "")
			  ;
			e.commit();

			StreamFileClientFragment.setDefaultValues(ctx, sharedPrefsName, true);
			StreamNtripClientFragment.setDefaultValues(ctx, sharedPrefsName, true);
			StreamTcpClientFragment.setDefaultValues(ctx, sharedPrefsName, true);
		}
	}

	static void setOutputStreamDefaultValues(Context ctx, String sharedPrefsName, boolean force) {
		final SharedPreferences prefs;

		prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

		final boolean needUpdate = force || !prefs.contains(OutputSolution1Fragment.KEY_ENABLE);

		if (needUpdate) {
			SharedPreferences.Editor e = prefs.edit();
			e.putBoolean(OutputSolution1Fragment.KEY_ENABLE, false)
			 .putString(OutputSolution1Fragment.KEY_TYPE, StreamType.NTRIPCLI.name())
			 .putString(OutputSolution1Fragment.KEY_FORMAT, SolutionFormat.LLH.name())
			  ;
			e.commit();
			StreamFileClientFragment.setDefaultValues(ctx, sharedPrefsName, true);
			StreamNtripClientFragment.setDefaultValues(ctx, sharedPrefsName, true);
			StreamTcpClientFragment.setDefaultValues(ctx, sharedPrefsName, true);
		}
	}

	static void setLogStreamDefaultValues(Context ctx, String sharedPrefsName, boolean force) {
		final SharedPreferences prefs;

		prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

		final boolean needUpdate = force || !prefs.contains(LogRoverFragment.KEY_ENABLE);

		if (needUpdate) {
			SharedPreferences.Editor e = prefs.edit();
			e.putBoolean(LogRoverFragment.KEY_ENABLE, false)
			 .putString(LogRoverFragment.KEY_TYPE, StreamType.NTRIPCLI.name())
			  ;
			e.commit();
			StreamFileClientFragment.setDefaultValues(ctx, sharedPrefsName, true);
			StreamNtripClientFragment.setDefaultValues(ctx, sharedPrefsName, true);
			StreamTcpClientFragment.setDefaultValues(ctx, sharedPrefsName, true);
		}

	}

	@Nonnull
	static InputStream readInputStreamPrefs(Context ctx, String sharedPrefsName) {
		final SharedPreferences prefs;
		final InputStream stream;
		StreamType type;

		stream = new InputStream();
		prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

		if (!prefs.contains(InputRoverFragment.KEY_TYPE)) throw new IllegalStateException();

		if (!prefs.getBoolean(InputRoverFragment.KEY_ENABLE, false)) {
			stream.setType(StreamType.NONE);
			return stream;
		}

		type = StreamType.valueOf(prefs.getString(InputRoverFragment.KEY_TYPE, null));
		stream
			.setType(type)
			.setFormat(StreamFormat.valueOf(prefs.getString(InputRoverFragment.KEY_FORMAT, StreamFormat.RTCM3.name())))
			.setCommandsAtStartup(prefs.getString(InputRoverFragment.KEY_COMMANDS_AT_STARTUP, ""))
			.setReceiverOption(prefs.getString(InputRoverFragment.KEY_RECEIVER_OPTION, ""))
		;

		stream.setPath(readStreamPath(type, prefs));

		return stream;
	}

	public static OutputStream readOutputStreamPrefs(Context ctx,
			String sharedPrefsName, SolutionOptions base) {
		final SharedPreferences prefs;
		final OutputStream stream;
		StreamType type;

		stream = new OutputStream();
		stream.setSolutionOptions(base);

		prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

		if (!prefs.contains(OutputSolution1Fragment.KEY_TYPE)) throw new IllegalStateException();

		if (!prefs.getBoolean(OutputSolution1Fragment.KEY_ENABLE, false)) {
			stream.setType(StreamType.NONE);
			return stream;
		}

		type = StreamType.valueOf(prefs.getString(OutputSolution1Fragment.KEY_TYPE, null));
		stream
			.setType(type)
			.setSolutionFormat(SolutionFormat.valueOf(prefs.getString(OutputSolution1Fragment.KEY_FORMAT,SolutionFormat.NMEA.name())))
		;

		stream.setPath(readStreamPath(type, prefs));

		return stream;
	}

	public static LogStream readLogStreamPrefs(Context ctx, String sharedPrefsName) {
		final SharedPreferences prefs;
		final LogStream stream;
		StreamType type;

		stream = new LogStream();

		prefs = ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

		if (!prefs.contains(LogRoverFragment.KEY_TYPE)) throw new IllegalStateException();

		if (!prefs.getBoolean(LogRoverFragment.KEY_ENABLE, false)) {
			stream.setType(StreamType.NONE);
			return stream;
		}

		type = StreamType.valueOf(prefs.getString(LogRoverFragment.KEY_TYPE, null));
		stream.setType(type);

		stream.setPath(readStreamPath(type, prefs));

		return stream;
	}

	@Nonnull
	static String readStreamPath(StreamType type, SharedPreferences prefs) {
		String path;

		switch(type) {
		case FILE:
			path = StreamFileClientFragment.readPath(prefs);
			break;
		case NTRIPCLI:
			path = StreamNtripClientFragment.readPath(prefs);
			break;
		case TCPCLI:
			path = StreamTcpClientFragment.readPath(prefs);
			break;
		case NONE:
			path="";
			break;
		default:
			throw new IllegalArgumentException();
		}
		return path;
	}

	@Nonnull
	static String readInputStreamSumary(SharedPreferences prefs) {
		StreamType type;
		type = StreamType.valueOf(prefs.getString(InputRoverFragment.KEY_TYPE, StreamType.NONE.name()));
		return readStreamSummary(type, prefs);
	}

	@Nonnull
	static String readOutputStreamSumary(SharedPreferences prefs) {
		StreamType type;
		type = StreamType.valueOf(prefs.getString(OutputSolution1Fragment.KEY_TYPE, StreamType.NONE.name()));
		return readStreamSummary(type, prefs);
	}

	@Nonnull
	static String readLogStreamSumary(SharedPreferences prefs) {
		StreamType type;
		type = StreamType.valueOf(prefs.getString(LogRoverFragment.KEY_TYPE, StreamType.NONE.name()));
		return readStreamSummary(type, prefs);
	}

	@Nonnull
	static String readStreamSummary(StreamType type, SharedPreferences prefs) {
		String summary;
		switch(type) {
		case FILE:
			summary = StreamFileClientFragment.readSummary(prefs);
			break;
		case NTRIPCLI:
			summary = StreamNtripClientFragment.readSummary(prefs);
			break;
		case TCPCLI:
			summary = StreamTcpClientFragment.readSummary(prefs);
			break;
		case NONE:
			summary="";
			break;
		default:
			throw new IllegalArgumentException();
		}
		return summary;
	}

}
