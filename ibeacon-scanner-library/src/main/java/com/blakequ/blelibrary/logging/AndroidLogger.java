package com.blakequ.blelibrary.logging;

import android.util.Log;

/**
 * Copyright (C) BlakeQu All Rights Reserved <blakequ@gmail.com>
 * <p/>
 * Licensed under the blakequ.com License, Version 1.0 (the "License");
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * author  : quhao <blakequ@gmail.com> <br>
 * date     : 2016/6/3 15:02 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class AndroidLogger implements Logger{
    protected String formatString(String message, Object... args) {
        // If no varargs are supplied, treat it as a request to log the string without formatting.
        return args.length == 0 ? message : String.format(message, args);
    }

    @Override
    public void v(String tag, String message, Object... args) {
        Log.v(tag, formatString(message, args));
    }

    @Override
    public void v(Throwable t, String tag, String message, Object... args) {
        Log.v(tag, formatString(message, args), t);
    }

    @Override
    public void d(String tag, String message, Object... args) {
        Log.d(tag, formatString(message, args));
    }

    @Override
    public void d(Throwable t, String tag, String message, Object... args) {
        Log.d(tag, formatString(message, args), t);
    }

    @Override
    public void i(String tag, String message, Object... args) {
        Log.i(tag, formatString(message, args));
    }

    @Override
    public void i(Throwable t, String tag, String message, Object... args) {
        Log.i(tag, formatString(message, args), t);
    }

    @Override
    public void w(String tag, String message, Object... args) {
        Log.w(tag, formatString(message, args));
    }

    @Override
    public void w(Throwable t, String tag, String message, Object... args) {
        Log.w(tag, formatString(message, args), t);
    }

    @Override
    public void e(String tag, String message, Object... args) {
        Log.e(tag, formatString(message, args));
    }

    @Override
    public void e(Throwable t, String tag, String message, Object... args) {
        Log.e(tag, formatString(message, args), t);
    }
}
