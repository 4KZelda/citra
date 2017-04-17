package org.citraemu.citraemu.utils;

import android.graphics.Bitmap;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import org.citraemu.citraemu.NativeLibrary;

import java.io.IOException;
import java.nio.IntBuffer;

public class GameBannerRequestHandler extends RequestHandler {
	@Override
	public boolean canHandleRequest(Request data) {
		return "3ds".equals(data.uri.getScheme());
	}

	@Override
	public Result load(Request request, int networkPolicy) throws IOException {
		String url = request.uri.getHost() + request.uri.getPath();
		int[] vector = NativeLibrary.GetIcon(url, true);
		int width = 96;
		int height = 32;
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(vector, 0, width, 0, 0, width, height);
		bitmap.copyPixelsFromBuffer(IntBuffer.wrap(vector));
		return new Result(bitmap, Picasso.LoadedFrom.DISK);
	}
}
