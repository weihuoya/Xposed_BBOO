package com.weihuoya.bboo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

/**
 * Created by zhangwei on 2016/4/23.
 */
public class BasicRequestQueue {
    private static BasicRequestQueue mInstance;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    private BasicRequestQueue() {
        mRequestQueue = Volley.newRequestQueue(_G.getContext().getApplicationContext());
        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> mBitmapCache = new LruCache<>(16);

            @Override
            public Bitmap getBitmap(String url) {
                return mBitmapCache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                mBitmapCache.put(url, bitmap);
            }
        });
    }

    public static synchronized BasicRequestQueue getInstance() {
        if(mInstance == null) {
            mInstance = new BasicRequestQueue();
        }
        return mInstance;
    }

    public <T> void add(Request<T> request) {
        mRequestQueue.add(request);
    }

    public void add(final ImageView view, final String url, final int defaultImageResId, final int errorImageResId) {
        mImageLoader.get(url, ImageLoader.getImageListener(view, defaultImageResId, errorImageResId));
    }

    public void getJson(String url, Response.Listener<JSONObject> listener) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                listener,
                new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("zhangwei", error.toString());
            }
        });

        add(jsonObjectRequest);
    }
}
