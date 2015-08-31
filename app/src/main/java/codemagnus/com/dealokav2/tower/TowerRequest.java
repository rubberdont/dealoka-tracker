package codemagnus.com.dealokav2.tower;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by codemagnus on 3/25/2015.
 */
public class TowerRequest {

    public static final String TOWERS = "towers";

    private String id;
    private JSONObject post;
    private JSONArray postItems;
    private String status;
    private String date;

    public TowerRequest() {

    }

    public TowerRequest(JSONObject requestInfo) {
        try {
            setPost(requestInfo);
            setPostItems(requestInfo.getJSONArray(TOWERS));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JSONObject getPost() {
        return post;
    }

    public void setPost(JSONObject post) {
        this.post = post;
    }

    public JSONArray getPostItems() {
        return postItems;
    }

    public void setPostItems(JSONArray postItems) {
        this.postItems = postItems;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
