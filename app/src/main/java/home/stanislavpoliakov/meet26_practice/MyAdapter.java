package home.stanislavpoliakov.meet26_practice;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private static final String TAG = "meet26_logs";
    private List<Alarm> alarmList;
    private ICallback mActivity;

    public MyAdapter(Context context, Set<Alarm> alarmSet) {
        this.alarmList = new ArrayList<>(alarmSet);

        try {
            mActivity = (ICallback) context;
        } catch (ClassCastException ex) {
            Log.w(TAG, "MyAdapter: Activity must implement ICallback interface", ex);
        }
    }

    public void setData(Set<Alarm> alarmSet) {
        this.alarmList = new ArrayList<>(alarmSet);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder,
                parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Alarm alarm = alarmList.get(position);

        Calendar alarmStart = alarm.getStart();
        int repeatIn = alarm.getRepeatIn();

        //StringBuilder time = new StringBuilder();
        /*time.append(alarmStart.get(Calendar.HOUR_OF_DAY))
                .append(":")
                .append(alarmStart.get(Calendar.MINUTE));*/
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");


        holder.alarmSwitch.setChecked(alarm.isEnabled());
        holder.timeLabel.setText(dateFormat.format(alarmStart.getTime()));
        holder.repeatLabel.setText(String.valueOf(alarm.id));
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView timeLabel, repeatLabel;
        public Switch alarmSwitch;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            timeLabel = itemView.findViewById(R.id.timeLabel);
            repeatLabel = itemView.findViewById(R.id.repeatLabel);
            alarmSwitch = itemView.findViewById(R.id.alarmSwitch);

            itemView.setOnClickListener(v -> mActivity.itemSelected(alarmList.get(getAdapterPosition())));

            itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                    //Alarm alarm = alarmList.get(getAdapterPosition());
                    menu.add(0, getAdapterPosition(), 0, "Delete");
            });

            alarmSwitch.setOnClickListener(v -> {
                mActivity.switchChange(getAdapterPosition());
            });
        }
    }
}
