package gpsplus.rtkgps.utils;

import gpsplus.rtkgps.ToolsActivity.DownloaderCaller;

public interface IDownloaderAccessResponse {
    void postResult(String asyncresult, DownloaderCaller caller);
}