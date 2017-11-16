package com.pinssible.camerarecorder.camerarecorderdemo;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

/**
 * Created by ZhangHaoSong on 2017/11/16.
 */

public class FunctionViewBuilder {
    public static View getFunctionView(Activity context, String name, String description, View.OnClickListener listener) {
        View functionView = context.getLayoutInflater().inflate(R.layout.layout_function_item, null);
        ((TextView) functionView.findViewById(R.id.tv_name)).setText(name);
        ((TextView) functionView.findViewById(R.id.tv_description)).setText(description);
        functionView.setOnClickListener(listener);
        return functionView;
    }

}
