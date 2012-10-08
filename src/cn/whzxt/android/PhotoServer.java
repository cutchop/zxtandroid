package cn.whzxt.android;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public class PhotoServer extends NanoHTTPD {
	private StreamIt streamIt = null;
	public int ConCount = 0;
	public int PreConCount = 0;

	public PhotoServer(int port, StreamIt streamIt) throws IOException {
		super(port, null);
		this.streamIt = streamIt;
	}

	public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
		if (uri.equalsIgnoreCase("/shot.jpg")) {
			ConCount++;
			if (ConCount > 100000)
				ConCount = 1;
			if(!streamIt.Screenshot)
			{
				streamIt.Screenshot = true;
			}
			if (streamIt.yuv420sp != null) {
				return new Response(HTTP_OK, "image/jpeg", new ByteArrayInputStream(streamIt.yuv420sp));
			}
		}
		return null;
	}
}
