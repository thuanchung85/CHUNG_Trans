package nie.translator.rtranslatordevedition;

import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

public class TranslatorAPI {

    private static final String OCP_APIM_SUBSCRIPTION_KEY = "35874b044687447aaa3b7864b975157d";
    private static final String OCP_APIM_SUBSCRIPTION_REGION = "koreacentral";

//    private String BASE_URL = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=en&to=vi";

    public void translateText(String textToTranslate,String input , String output, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "[{\"Text\": \"" + textToTranslate + "\"}]");
        Log.d("TTT TranslatorAPI ", "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from="+input+"&to="+output);
        Request request = new Request.Builder()
                .url("https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from="+input+"&to="+output)
                .addHeader("Ocp-Apim-Subscription-Key", OCP_APIM_SUBSCRIPTION_KEY)
                .addHeader("Ocp-Apim-Subscription-Region", OCP_APIM_SUBSCRIPTION_REGION)
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }
}
