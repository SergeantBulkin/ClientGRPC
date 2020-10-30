package by.sergeantbulkin.grpc.model;

import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class MyWebViewClient extends WebViewClient
{
    TextView result;

    public MyWebViewClient(TextView binding)
    {
        this.result = binding;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request)
    {
        view.loadUrl(request.getUrl().toString());
        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url)
    {
        result.setText(view.getTitle());
        super.onPageFinished(view, url);
    }

}
