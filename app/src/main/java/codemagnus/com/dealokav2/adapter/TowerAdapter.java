package codemagnus.com.dealokav2.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import codemagnus.com.dealokav2.R;
import codemagnus.com.dealokav2.tower.Tower;
import codemagnus.com.dealokav2.utils.GeneralUtils;


/**
 * Created by eharoldreyes on 11/22/14.
 */
public class TowerAdapter extends BaseAdapter {

    private final Context context;
    private ArrayList<Tower> towers;

    public TowerAdapter(Context context, ArrayList<Tower> towers){
        this.context = context;
        this.towers = towers;
    }

    public void setTowers(ArrayList<Tower> towers){
        this.towers = towers;
        notifyDataSetChanged();
    }

    public ArrayList<Tower> getTowers() {
        return this.towers;
    }

    static class ViewHolder {
        public TextView time, signal, lac, cid, networkType, servsTime, level, status;
        public LinearLayout llTowers;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        Tower tower = towers.get(position);

        if(tower.isNeighbor()) {
            convertView = LayoutInflater.from(context).inflate(R.layout.row_towers_null, null);
        } else {
            if(convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.row_tower, parent, false);
                holder = getHolderViews(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
                if(holder == null) {
                    View v = LayoutInflater.from(context).inflate(R.layout.row_tower, parent, false);
                    holder = getHolderViews(v);
                }
            }

            if(holder != null && holder.time != null) {
                holder.time         .setText("" + tower.getTime());
                holder.signal       .setText("" + tower.getRssi() + " dBm");
                holder.lac          .setText("" + tower.getLocationAreaCode());
                holder.cid          .setText("" + tower.getCellId());
                holder.networkType  .setText("" + tower.getNetworkType());
                holder.level        .setText("" + GeneralUtils.calculateBtsSignal(tower.getRssi()));
//                holder.servsTime    .setText("" + (tower.getMillis().length() > 0 ? Math.round(Long.parseLong(tower.getMillis()) / 1000) : ""));
                holder.servsTime    .setText("" + tower.getMillis());

                holder.status       .setText("" + tower.getPostStatus());

                holder.llTowers.setBackgroundColor(tower.isPosted() ? context.getResources().getColor(R.color.darkgreen):0);
            }
        }

        return convertView;
    }

    private ViewHolder getHolderViews(View convertView) {
        ViewHolder  holder 				= new ViewHolder();
        holder.time         = (TextView) convertView.findViewById(R.id.tv_time);
        holder.signal       = (TextView) convertView.findViewById(R.id.tv_signal);
        holder.lac          = (TextView) convertView.findViewById(R.id.tv_lac);
        holder.cid          = (TextView) convertView.findViewById(R.id.tv_cid);
        holder.networkType  = (TextView) convertView.findViewById(R.id.tv_network_type);
        holder.level        = (TextView) convertView.findViewById(R.id.tv_level);
        holder.servsTime    = (TextView) convertView.findViewById(R.id.tv_serve_time);
        holder.status       = (TextView) convertView.findViewById(R.id.tv_status);
        holder.llTowers     = (LinearLayout) convertView.findViewById(R.id.ll_towers);

        return holder;
    }

    @Override
    public int getCount() {
        return towers.size();
    }

    @Override
    public Tower getItem(int position) {
        return towers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


    public void clear(){
        this.towers = new ArrayList<>();
        notifyDataSetInvalidated();
        notifyDataSetChanged();
    }

}
