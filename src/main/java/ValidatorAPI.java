import dataTypes.ValidatorResult;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ValidatorAPI {
    //Default validation, automatically detect charset and doctype
    @GET("check?charset=(detect+automatically)&doctype=Inline&group=0&output=json")
    Call<ValidatorResult> getTestResult(@Query("uri") String file);

    //Force validation with UTF-8 charset
    @GET("check?charset=utf-8&doctype=Inline&group=0&output=json")
    Call<ValidatorResult> getTestResultUTF8(@Query("uri") String file);

    //Force validation with HTML5 doctype
    @GET("check?charset=(detect+automatically)&doctype=HTML5&group=0&output=json")
    Call<ValidatorResult> getTestResultHTML5(@Query("uri") String file);

    //Force validation with UTF-8 charset and HTML5 doctype
    @GET("check?charset=utf-8&doctype=HTML5&group=0&output=json")
    Call<ValidatorResult> getTestResultUTF8HTML5(@Query("uri") String file);
}