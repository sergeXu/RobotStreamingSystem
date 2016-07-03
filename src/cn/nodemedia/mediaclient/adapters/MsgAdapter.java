package cn.nodemedia.mediaclient.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import cn.nodemedia.mediaclient.R;
import cn.nodemedia.mediaclient.models.Msg;

public class MsgAdapter extends ArrayAdapter<Msg> {

    private int resourceId;


    public MsgAdapter(Context context, int textViewResourceId, List<Msg> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Msg msg = getItem(position);
        View view;
        ViewHolder viewHolder;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(resourceId, null);
            viewHolder = new ViewHolder();
            viewHolder.leftLayout = (LinearLayout) view.findViewById(R.id.left_layout);
            viewHolder.rightLayout = (LinearLayout) view.findViewById(R.id.right_layout);
            viewHolder.leftMsg = (TextView) view.findViewById(R.id.left_msg);
            viewHolder.rightMsg = (TextView) view.findViewById(R.id.right_msg);
            viewHolder.leftoutLayout = (LinearLayout) view.findViewById(R.id.left_outlayout);
            viewHolder.rightoutLayout = (LinearLayout) view.findViewById(R.id.right_outlayout);
            viewHolder.left_msg_person = (TextView) view.findViewById(R.id.left_msg_person);
            viewHolder.right_msg_person = (TextView) view.findViewById(R.id.right_msg_person);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        if (msg.getType() == Msg.TYPE_RECEIVED) {
            viewHolder.leftoutLayout.setVisibility(View.VISIBLE);
            viewHolder.leftLayout.setVisibility(View.VISIBLE);
            viewHolder.rightoutLayout.setVisibility(View.GONE);
            viewHolder.rightLayout.setVisibility(View.GONE);
            viewHolder.leftMsg.setText(msg.getContent());
            viewHolder.left_msg_person.setText(msg.getPersonName() + " ");
        } else if (msg.getType() == Msg.TYPE_SENT) {
            viewHolder.rightoutLayout.setVisibility(View.VISIBLE);
            viewHolder.rightLayout.setVisibility(View.VISIBLE);
            viewHolder.leftoutLayout.setVisibility(View.GONE);
            viewHolder.leftLayout.setVisibility(View.GONE);
            viewHolder.rightMsg.setText(msg.getContent());
            viewHolder.right_msg_person.setText(" " + msg.getPersonName());
        }
        return view;
    }

    class ViewHolder {

        LinearLayout leftLayout;

        LinearLayout rightLayout;

        TextView leftMsg;

        TextView rightMsg;

        LinearLayout leftoutLayout;

        LinearLayout rightoutLayout;

        TextView right_msg_person;

        TextView left_msg_person;

    }

}
