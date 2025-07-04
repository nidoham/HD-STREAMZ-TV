package com.nidoham.hdstreamztv.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nidoham.hdstreamztv.PlayerActivity;
import com.nidoham.hdstreamztv.R;
import com.nidoham.hdstreamztv.model.ChannelUrl;
import java.util.List;

public class MultipleLinkDialog {
    
    private static String URL = "";

    public static void show(Context context, List<ChannelUrl> urls) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_multiple_links);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView link1 = dialog.findViewById(R.id.link1);
        TextView link2 = dialog.findViewById(R.id.link2);
        TextView link3 = dialog.findViewById(R.id.link3);
        TextView link4 = dialog.findViewById(R.id.link4);
        TextView link5 = dialog.findViewById(R.id.link5);
        
        int length = urls.size();
        
        if(length == 1) {
        	String link = urls.get(0).getTittle();
            URL = urls.get(0).getLink();
            link1.setText(link);
        } else {
            link1.setVisibility(View.GONE);
        }
        
        if(length == 2) {
        	final String link = urls.get(1).getTittle();
            URL = urls.get(1).getLink();
            link2.setText(link);
        } else {
            link2.setVisibility(View.GONE);
        }
        
        if(length == 3) {
        	String link = urls.get(2).getTittle();
            URL = urls.get(2).getLink();
            link3.setText(link);
        } else {
            link3.setVisibility(View.GONE);
        }
        
        if(length == 4) {
        	String link = urls.get(3).getTittle();
            URL = urls.get(3).getLink();
            link4.setText(link);
        } else {
            link4.setVisibility(View.GONE);
        }
        
        if(length == 5) {
        	String link = urls.get(4).getTittle();
            URL = urls.get(4).getLink();
            link5.setText(link);
        } else {
            link5.setVisibility(View.GONE);
        }

        View.OnClickListener listener = v -> {
            Intent intent = new Intent();
            intent.putExtra("link", URL);
            intent.setClass(context, PlayerActivity.class);
            context.startActivity(intent);
            dialog.dismiss();
        };

        link1.setOnClickListener(listener);
        link2.setOnClickListener(listener);
        link3.setOnClickListener(listener);
        link4.setOnClickListener(listener);
        link5.setOnClickListener(listener);

        dialog.show();
    }
}