package tcss450.uw.edu.messengerapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tcss450.uw.edu.messengerapp.model.ListenManager;
import tcss450.uw.edu.messengerapp.utils.SendPostAsyncTask;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragment newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment {

    private String mUsername;
    private String mSendUrl;
    private TextView mOutputTextView;
    private ListenManager mListenManager;

    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        v.findViewById(R.id.chatSendButton).setOnClickListener(this::sendMessage);
        mOutputTextView = v.findViewById(R.id.chatOutput);


        //do postaysync task and grab all messages and to output

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs =
                getActivity().getSharedPreferences(
                        getString(R.string.keys_shared_prefs),
                        Context.MODE_PRIVATE);
        if (!prefs.contains(getString(R.string.keys_prefs_username))) {
            throw new IllegalStateException("No username in prefs!");
        }
        mUsername = prefs.getString(getString(R.string.keys_prefs_username), "");

        mSendUrl = new Uri.Builder()
                .scheme("https")
                .appendPath(getString(R.string.ep_base_url))
                .appendPath(getString(R.string.ep_send_message))
                .build()
                .toString();

//        Uri retrieve;
//        Uri.Builder builder = new Uri.Builder();
//        builder.scheme("https");
//        builder.appendPath(getString(R.string.ep_base_url));
//        builder.appendPath(getString(R.string.ep_get_message));
//        builder.appendQueryParameter("chatId", "1");
//        builder.appendQueryParameter("after", "1970-01-01 00:00:00.000000");

//        retrieve = builder.build();
        Uri retrieve = new Uri.Builder()
                .scheme("https")
                .appendPath(getString(R.string.ep_base_url))
                .appendPath(getString(R.string.ep_get_message))
                .appendQueryParameter("chatId", "1")

                .build();


        if (prefs.contains(getString(R.string.keys_prefs_time_stamp))) {
            //ignore all of the seen messages. You may want to store these messages locally
            mListenManager = new ListenManager.Builder(retrieve.toString(),
                    this::publishProgress)
                    .setTimeStamp(prefs.getString(getString(R.string.keys_prefs_time_stamp), "0"))
                    .setExceptionHandler(this::handleError)
                    .setDelay(1000)
                    .build();
        } else {
            //no record of a saved timestamp. must be a first time login
            mListenManager = new ListenManager.Builder(retrieve.toString(),
                    this::publishProgress)
                    .setExceptionHandler(this::handleError)
                    .setDelay(1000)
                    .build();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mListenManager.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        String latestMessage = mListenManager.stopListening();
        SharedPreferences prefs =
                getActivity().getSharedPreferences(
                        getString(R.string.keys_shared_prefs),
                        Context.MODE_PRIVATE);
        // Save the most recent message timestamp
        prefs.edit().putString(
                getString(R.string.keys_prefs_time_stamp),
                latestMessage)
                .apply();
    }



    private void sendMessage(final View theButton) {
        JSONObject messageJson = new JSONObject();
        String msg = ((EditText) getView().findViewById(R.id.chatInput))
                .getText().toString();

        try {
            messageJson.put(getString(R.string.keys_json_username), mUsername);
            messageJson.put(getString(R.string.keys_json_message), msg);
            messageJson.put(getString(R.string.keys_json_chat_id), 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new SendPostAsyncTask.Builder(mSendUrl, messageJson)
                .onPostExecute(this::endOfSendMsgTask)
                .onCancelled(this::handleError)
                .build().execute();
    }

    private void handleError(final String msg) {
        Log.e("CHAT ERROR!!!", msg.toString());
    }

    private void endOfSendMsgTask(final String result) {
        try {
            JSONObject res = new JSONObject(result);

            if(res.get(getString(R.string.keys_json_success)).toString()
                    .equals(getString(R.string.keys_json_success_value_true))) {

                ((EditText) getView().findViewById(R.id.chatInput))
                        .setText("");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleError(final Exception e) {
        Log.e("LISTEN ERROR!!!", e.getMessage());
    }

    private void publishProgress(JSONObject messages) {
        final String[] msgs;
        if(messages.has(getString(R.string.keys_json_messages))) {
            try {

                JSONArray jMessages = messages.getJSONArray(getString(R.string.keys_json_messages));

                msgs = new String[jMessages.length()];
                for (int i = 0; i < jMessages.length(); i++) {

                    JSONObject msg = jMessages.getJSONObject(i);
                    String username = msg.get(getString(R.string.keys_json_username)).toString();
                    String userMessage = msg.get(getString(R.string.keys_json_message)).toString();
                    msgs[i] = username + ":" + userMessage;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }

            getActivity().runOnUiThread(() -> {
                for (String msg : msgs) {
                    Log.e("HERE new ARE THE MESSAGES", msg);
                    mOutputTextView.append(msg);
                    mOutputTextView.append(System.lineSeparator());
                }
            });
        }
    }



}


//
//
//package tcss450.uw.edu.messengerapp;
//
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.net.Uri;
//import android.os.Bundle;
//import android.support.v4.app.Fragment;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.EditText;
//import android.widget.TextView;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import tcss450.uw.edu.messengerapp.utils.ListenManager;
//import tcss450.uw.edu.messengerapp.utils.SendPostAsyncTask;
//
//
///**
// * A simple {@link Fragment} subclass.
// */
//public class ChatFragment extends Fragment {
//    private String mUsername;
//    private String mSendUrl;
//    private TextView mOutputTextView;
//    private ListenManager mListenManager;
//
//    public ChatFragment() {
//        // Required empty public constructor
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        SharedPreferences prefs =
//                getActivity().getSharedPreferences(
//                        getString(R.string.keys_shared_prefs),
//                        Context.MODE_PRIVATE);
//        if (!prefs.contains(getString(R.string.keys_prefs_username))) {
//            throw new IllegalStateException("No username in prefs!");
//        }
//        mUsername = prefs.getString(getString(R.string.keys_prefs_username), "");
//        mSendUrl = new Uri.Builder()
//                .scheme("https")
//                .appendPath(getString(R.string.ep_base_url))
//                .appendPath(getString(R.string.ep_send_message))
//                .build()
//                .toString();
//        Uri retrieve = new Uri.Builder()
//                .scheme("https")
//                .appendPath(getString(R.string.ep_base_url))
//                .appendPath(getString(R.string.ep_get_message))
//                .appendQueryParameter("chatId", "1")
//                .build();
//        if (prefs.contains(getString(R.string.keys_prefs_time_stamp))) {
//            //ignore all of the seen messages. You may want to store these messages locally
//            mListenManager = new ListenManager.Builder(retrieve.toString(),
//                    this::publishProgress)
//                    .setTimeStamp(prefs.getString(getString(R.string.keys_prefs_time_stamp),
//                            "0"))
//                    .setExceptionHandler(this::handleError)
//                    .setDelay(1000)
//                    .build();
//        } else {
//            //no record of a saved timestamp. must be a first time login
//            mListenManager = new ListenManager.Builder(retrieve.toString(),
//                    this::publishProgress)
//                    .setExceptionHandler(this::handleError)
//                    .setDelay(1000)
//                    .build();
//        }
//    }
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View v = inflater.inflate(R.layout.fragment_chat, container, false);
//        v.findViewById(R.id.chatSendButton).setOnClickListener(this::sendMessage);
//        mOutputTextView = v.findViewById(R.id.chatOutput);
//        return v;
//    }
//    private void sendMessage(final View theButton) {
//        JSONObject messageJson = new JSONObject();
//        String msg = ((EditText) getView().findViewById(R.id.chatInput))
//                .getText().toString();
//        try {
//            messageJson.put(getString(R.string.keys_json_username), mUsername);
//            messageJson.put(getString(R.string.keys_json_message), msg);
//            messageJson.put(getString(R.string.keys_json_chat_id), 1);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        new SendPostAsyncTask.Builder(mSendUrl, messageJson)
//                .onPostExecute(this::endOfSendMsgTask)
//                .onCancelled(this::handleError)
//                .build().execute();
//    }
//    private void handleError(final String msg) {
//        Log.e("CHAT ERROR!!!", msg.toString());
//    }
//    private void endOfSendMsgTask(final String result) {
//        try {
//            JSONObject res = new JSONObject(result);
//            if(res.get(getString(R.string.keys_json_success)).toString()
//                    .equals(getString(R.string.keys_json_success_value_true))) {
//                ((EditText) getView().findViewById(R.id.chatInput))
//                        .setText("");
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//    private void handleError(final Exception e) {
//        Log.e("LISTEN ERROR!!!", e.getMessage());
//    }
//    private void publishProgress(JSONObject messages) {
//        final String[] msgs;
//        if(messages.has(getString(R.string.keys_json_messages))) {
//            try {
//                JSONArray jMessages =
//                        messages.getJSONArray(getString(R.string.keys_json_messages));
//                msgs = new String[jMessages.length()];
//                for (int i = 0; i < jMessages.length(); i++) {
//                    JSONObject msg = jMessages.getJSONObject(i);
//                    String username =
//                            msg.get(getString(R.string.keys_json_username)).toString();
//                    String userMessage =
//                            msg.get(getString(R.string.keys_json_message)).toString();
//                    msgs[i] = username + ":" + userMessage;
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//                return;
//            }
//            getActivity().runOnUiThread(() -> {
//                for (String msg : msgs) {
//                    mOutputTextView.append(msg);
//                    mOutputTextView.append(System.lineSeparator());
//                }
//            });
//        }
//    }
//    @Override
//    public void onResume() {
//        super.onResume();
//        mListenManager.startListening();
//    }
//    @Override
//    public void onStop() {
//        super.onStop();
//        String latestMessage = mListenManager.stopListening();
//        SharedPreferences prefs =
//                getActivity().getSharedPreferences(
//                        getString(R.string.keys_shared_prefs),
//                        Context.MODE_PRIVATE);
//        // Save the most recent message timestamp
//        prefs.edit().putString(
//                getString(R.string.keys_prefs_time_stamp),
//                latestMessage)
//                .apply();
//    }
//
//}
