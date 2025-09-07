package com.android.yt_legasy_a21;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.json.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class MainActivity extends Activity {

    EditText editSearch;
    Button btnSearch;
    ListView listView;
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> videoUrls = new ArrayList<String>();
    ArrayList<String> videoIds = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editSearch = (EditText) findViewById(R.id.editSearch);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        listView = (ListView) findViewById(R.id.listView);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(adapter);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String query = editSearch.getText().toString().trim();
                if (!query.equals("")) {
                    searchVideos(query);
                }
            }
        });

        // クリックリスナーは onCreate 内で1回だけ
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // クラッシュ防止
                if (position < 0 || position >= videoUrls.size()) {
                    Toast.makeText(MainActivity.this, "無効な選択です", Toast.LENGTH_SHORT).show();
                    return;
                }

                final String tappedVideoUrl = videoUrls.get(position); // MP4直リンク
                final String tappedVideoId = videoIds.get(position);

                Log.d("YTClient", "タップされたビデオID: " + tappedVideoId);
                Log.d("YTClient", "タップされたMP4直リンク: " + tappedVideoUrl);
                //preloadVideo(tappedVideoId);
                //Toast.makeText(MainActivity.this, "読み込み中", Toast.LENGTH_SHORT).show();
                //new android.os.Handler().postDelayed(new Runnable() {
                //    @Override
                //    public void run() {
                       playWithVLC(tappedVideoUrl);
                //    }
                //}, 5000);
            }
        });
    }

    private void searchVideos(final String query) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String encoded = URLEncoder.encode(query, "UTF-8");
                    Log.d("YTClient", "検索開始: " + query);
                    URL url = new URL("http://192.168.2.12:3000/api/v1/search?q=" + encoded);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    parseJson(sb.toString());

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("YTClient", "検索失敗", e);
                }
            }
        }).start();
    }

    private void parseJson(final String jsonStr) {
        try {
            JSONArray array = new JSONArray(jsonStr);
            Log.d("YTClient", "取得したJSON: " + jsonStr);

            titles.clear();
            videoUrls.clear();
            videoIds.clear();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                // videoId が存在しない場合はスキップ
                if (!obj.has("videoId")) continue;

                String title = obj.optString("title", "無題");
                String videoId = obj.getString("videoId");

                // MP4直リンクを取得。
                String videoUrl = "http://192.168.2.12:3000/latest_version?id=" + videoId;

                titles.add(title);
                videoUrls.add(videoUrl);
                videoIds.add(videoId);

                Log.d("YTClient", "title: " + title);
                Log.d("YTClient", "videoUrl: " + videoUrl);
                Log.d("YTClient", "videoId: " + videoId);
            }

            // UIスレッドで ListView 更新
            runOnUiThread(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                    Log.d("YTClient", "リスト更新完了: " + titles.size() + "件");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("YTClient", "JSONパース失敗", e);
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "JSONを展開できません", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    //プレロード機能(未使用)
    private void preloadVideo(String videoId) {
        final String preloadUrl = "http://192.168.2.12:3000/latest_version?id=" + videoId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(preloadUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    // レスポンスを読むが、画面には表示しない
                    InputStream is = conn.getInputStream();
                    while (is.read() != -1) {
                        // 読むだけで特に何もしない
                    }
                    is.close();
                    conn.disconnect();

                    Log.d("YTClient", "プレロード完了: " + preloadUrl);
                } catch (Exception e) {
                    Log.e("YTClient", "プレロード失敗: " + preloadUrl, e);
                }
            }
        }).start();
    }



    private void playWithVLC(String videoUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/mp4");
            intent.setPackage("org.videolan.vlc");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "VLCが見つかりません", Toast.LENGTH_SHORT).show();
        }
    }
}
