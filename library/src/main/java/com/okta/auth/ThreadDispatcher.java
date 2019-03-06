/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.okta.auth;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RestrictTo;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Executor Service that runs tasks on worker thread
 * call back on ui thread or specified executor.
 */
@RestrictTo(LIBRARY_GROUP)
public class ThreadDispatcher extends AbstractExecutorService {
    private boolean mShutdown = false;
    //executor used to run async requests
    private ExecutorService mExecutorService;

    //callback executor provide by app for callbacks
    private Executor mCallbackExecutor;

    //main handler for callbacks on main thread.
    private Handler mHandler;

    private synchronized ExecutorService getExecutorService() {
        if (mExecutorService == null) {
            mExecutorService = Executors.newSingleThreadExecutor();
        }
        return mExecutorService;
    }

    ThreadDispatcher(Executor callbackExecutor) {
        if (callbackExecutor == null) {
            mHandler = new Handler(Looper.getMainLooper());
        } else {
            mCallbackExecutor = callbackExecutor;
        }
    }

    @Override
    public void shutdown() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mCallbackExecutor instanceof ExecutorService) {
            ((ExecutorService) mCallbackExecutor).shutdown();
        }
        if (mExecutorService != null) {
            mExecutorService.shutdown();
        }
        mShutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return mShutdown;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return false;
    }

    @Override
    public void execute(Runnable command) {
        if (command instanceof RequestCallback) {
            android.util.Log.d("ThreadDispatcher", "command is instance of RequestCallback");
            //run callbacks on provided executor or ui thread.
            if (mCallbackExecutor != null) {
                mCallbackExecutor.execute(command);
            } else if (mHandler.getLooper() == Looper.myLooper()) {
                command.run();
            } else {
                mHandler.post(command);
            }
        } else {
            android.util.Log.d("ThreadDispatcher", "command is not a RequestCallback dispatchRequest");
            getExecutorService().submit(command);
        }
    }
}
