package com.douglasshirillasolutions.spotifytoyoutube;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    public static final String CLIENT_ID = keys.clientId;
    public static final String REDIRECT_URI = "spotify-sdk://auth";
    public static final int AUTH_TOKEN_REQUEST_CODE = 0x10;
    public static final int AUTH_CODE_REQUEST_CODE = 0x11;
    public static List<String> playlistItemsList = new ArrayList<>();


    private final OkHttpClient mOkHttpClient = new OkHttpClient();
    private String mAccessToken;
    private String mAccessCode;
    private Call mCall;
    public String userSpotifyId= "";
    boolean signedIntoSpotifyFlag;
    String selectedPlaylistId;
    List<String> playlistList = new ArrayList<>();
    //List<String> playlistItemsList = new ArrayList<>();
    List<String> playlistIdList = new ArrayList<>();
    Spinner spinner;
    Spinner playlistItemsSpinner;

    GoogleSignInClient mGoogleSignInClient;
    SignInButton sign_in_button;
    private GoogleApiClient googleApiClient;
    private static final int RC_SIGN_IN = 1;

    ProgressDialog mProgress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        //mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        //GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        spinner=(Spinner)findViewById(R.id.spinnerPlaylists);
        playlistItemsSpinner=(Spinner)findViewById(R.id.spinnerPlaylistItems);
        getSupportActionBar().setTitle(String.format(
                Locale.US, "Spotify to Youtube %s", com.spotify.sdk.android.auth.BuildConfig.VERSION_NAME));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String playlist =   spinner.getItemAtPosition(spinner.getSelectedItemPosition()).toString();
                Toast.makeText(getApplicationContext(),playlist,Toast.LENGTH_LONG).show();
                selectedPlaylistId = playlistIdList.get(i);
                setResponse(spinner.getItemAtPosition(spinner.getSelectedItemPosition()).toString());
                GetPlayListItems();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // DO Nothing here
            }
        });

        Button signIntoYoutube = findViewById(R.id.btn_signIntoYoutube);
        signIntoYoutube.setOnClickListener(v -> {
            Intent intent = new Intent(this,youtubeSignIn.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        cancelCall();
        super.onDestroy();
    }

    public void onGetPlaylistsClicked(View view) {
        if (mAccessToken == null) {
            final Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_main), R.string.warning_need_token, Snackbar.LENGTH_SHORT);
            snackbar.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
            snackbar.show();
            return;
        }

        final Request request = new Request.Builder()
                .url("https://api.spotify.com/v1/me")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        cancelCall();
        mCall = mOkHttpClient.newCall(request);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                setResponse("Failed to fetch data: " + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    final JSONObject jsonObject = new JSONObject(response.body().string());
                    //setResponse(jsonObject.toString(3));
                    userSpotifyId = jsonObject.getString("id");
                    //GetPlayLists(view);
                } catch (JSONException e) {
                    setResponse("Failed to parse data: " + e);
                }
            }
        });

    }
    public void GetPlayLists(){
        if (mAccessToken == null) {
            final Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_main), R.string.warning_need_token, Snackbar.LENGTH_SHORT);
            snackbar.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
            snackbar.show();
            return;
        }

        final Request requestTwo = new Request.Builder()
                .url("https://api.spotify.com/v1/users/" + userSpotifyId + "/playlists?limit=50")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        cancelCall();
        mCall = mOkHttpClient.newCall(requestTwo);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                setResponse("Failed to fetch data: " + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    final JSONObject jsonObject = new JSONObject(response.body().string());
                    Log.d("playlist json", jsonObject.toString());
                    //setResponse(jsonObject.toString(3));
                    updatePlaylistView(jsonObject);

                } catch (JSONException e) {
                    setResponse("Failed to parse data: " + e);
                }
            }
        });
    }
    public void GetPlayListItems(){
        if (mAccessToken == null) {
            final Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_main), R.string.warning_need_token, Snackbar.LENGTH_SHORT);
            snackbar.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
            snackbar.show();
            return;
        }

        final Request requestTwo = new Request.Builder()
                .url("https://api.spotify.com/v1/playlists/" + selectedPlaylistId + "/tracks")
                .addHeader("Authorization", "Bearer " + mAccessToken)
                .build();

        cancelCall();
        mCall = mOkHttpClient.newCall(requestTwo);

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                setResponse("Failed to fetch data: " + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    final JSONObject jsonObject = new JSONObject(response.body().string());
                    Log.d("playlist json", jsonObject.toString());
                    //setResponse(jsonObject.toString(3));
                    updatePlaylistSpinner(jsonObject);

                } catch (JSONException e) {
                    setResponse("Failed to parse data: " + e);
                }
            }
        });
    }
    private void updatePlaylistView(JSONObject jsonObject) {
        //final TextView playlistView = findViewById(R.id.playlists_text_view);
        //playlistView.setText(getString(R.string.token, userSpotifyId));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                JSONObject playlistJson = null;
                try {
                    playlistJson = new JSONObject(jsonObject.toString());
                    if (playlistJson != null) {
                        JSONArray jsonArray = playlistJson.getJSONArray("items");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                            String playlistName = jsonObject1.getString("name");
                            String playlistId = jsonObject1.getString("id");
                            //playlistList.clear();
                            playlistList.add(playlistName);
                            playlistIdList.add(playlistId);
                        }
                        spinner.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, playlistList));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }



    private void updatePlaylistSpinner(JSONObject jsonObject) {
        //final TextView playlistView = findViewById(R.id.playlists_text_view);
        //playlistView.setText(getString(R.string.token, userSpotifyId));
        playlistItemsList.clear();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                JSONObject playlistJson = null;
                try {
                    playlistJson = new JSONObject(jsonObject.toString());
                    if (playlistJson != null) {
                        JSONArray jsonArray = playlistJson.getJSONArray("items");

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject1 = jsonArray.getJSONObject(i);

                            JSONObject track = jsonObject1.getJSONObject("track");

                            String songName = track.getString("name");
                            String artistName= "poo";
                            JSONArray artists = track.getJSONArray("artists");

                            for (int j=0;j<artists.length();j++){
                                JSONObject result = artists.getJSONObject(j);
                                artistName = result.getString("name");
                                //artistName = artistNames.getString("name");
                            }

                            String songInfo = songName + " - " + artistName;
                            playlistItemsList.add(songInfo);
                            }
                        // TODO: 5/24/2022 implement youtube search functionality to choose top result for each query

                        }
                    playlistItemsSpinner.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, playlistItemsList));
                    }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onSignIntoSpotifyClicked(View view) {

        //get token
        final AuthorizationRequest request = getAuthenticationRequest(AuthorizationResponse.Type.TOKEN);
        AuthorizationClient.openLoginActivity(this, AUTH_TOKEN_REQUEST_CODE, request);
    }

    public void onClearCredentialsClicked(View view) {
        AuthorizationClient.clearCookies(this);
    }

    private AuthorizationRequest getAuthenticationRequest(AuthorizationResponse.Type type) {
        return new AuthorizationRequest.Builder(CLIENT_ID, type, getRedirectUri().toString())
                .setShowDialog(false)
                .setScopes(new String[]{"playlist-read-private"})
                .setCampaign("your-campaign-token")
                .build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, data);


        if (AUTH_TOKEN_REQUEST_CODE == requestCode) {
            mAccessToken = response.getAccessToken();

            //get code
            final AuthorizationRequest request = getAuthenticationRequest(AuthorizationResponse.Type.CODE);
            AuthorizationClient.openLoginActivity(this, AUTH_CODE_REQUEST_CODE, request);

        } else if (AUTH_CODE_REQUEST_CODE == requestCode) {
            mAccessCode = response.getCode();
            //Thread.sleep(2000);
            //signedIntoSpotifyFlag = true;
            //onGetPlaylistsClicked();
        }
        else if (AUTH_CODE_REQUEST_CODE == RC_SIGN_IN) {
            Task task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                handleSignInResult(task);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        if(mAccessToken != null && mAccessCode != null){
            String u = userSpotifyId;
            String t = mAccessToken;
            final Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/me")
                    .addHeader("Authorization", "Bearer " + mAccessToken)
                    .build();

            cancelCall();
            mCall = mOkHttpClient.newCall(request);

            mCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    setResponse("Failed to fetch data: " + e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        final JSONObject jsonObject = new JSONObject(response.body().string());
                        //setResponse(jsonObject.toString(3));
                        userSpotifyId = jsonObject.getString("id");
                        GetPlayLists();
                    } catch (JSONException e) {
                        setResponse("Failed to parse data: " + e);
                    }
                }
            });
        }
    }
    private void handleSignInResult(Task completedTask) throws Throwable {
        try {
            GoogleSignInAccount account = (GoogleSignInAccount) completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            //updateUI();

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            //Log.w("Warning", "signInResult:failed code=" + e.getStatusCode());

        }
    }


    private void setResponse(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView responseView = findViewById(R.id.playlists_text_view);
                responseView.setText(text);
            }
        });
    }



    private void cancelCall() {
        if (mCall != null) {
            mCall.cancel();
        }
    }

    private Uri getRedirectUri() {
        return Uri.parse(REDIRECT_URI);
    }

//begin google/youtube sign in



    public void onSignIntoYoutubeClicked(View view) {

        //switch (view.getId()) {
        //    case R.id.btn_signIntoYoutube:
        //        signInToGoogle();
        //        break;

        //}
        //mProgress = new ProgressDialog(this);
        //mProgress.setMessage("Calling YouTube Data API ...");
        // Initialize credentials and service object.

        //getResultsFromApi();
       //YouTube.Playlists.List request = youtubeService.playlists().list("");
        //Intent intent = new Intent(this, youtubeSignIn.class);
        //startActivity(intent);
    }


    private void signInToGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);

        //gso.getScopes();
        //gso.getAccount();
    }

}