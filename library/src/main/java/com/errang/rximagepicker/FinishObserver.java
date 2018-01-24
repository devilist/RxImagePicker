package com.errang.rximagepicker;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;

/**
 * Created by zengp on 2017/7/9.
 */

class FinishObserver<T> extends DisposableObserver<T> {

    @Override
    public void onNext(@NonNull Object o) {

    }

    @Override
    public void onError(@NonNull Throwable e) {
        onFinish();
    }

    @Override
    public void onComplete() {
        onFinish();
    }

    public void onFinish() {

    }
}
