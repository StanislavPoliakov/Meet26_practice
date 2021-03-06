package home.stanislavpoliakov.meet26_practice;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");

        holder.alarmSwitch.setChecked(alarm.isEnabled());
        holder.timeLabel.setText(dateFormat.format(alarmStart.getTime()));
        holder.repeatLabel.setText(String.valueOf(alarm.getRepeatString()));
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

            // Устанавливаем обработчик нажатий на элемент списка (для обновления будильника)
            itemView.setOnClickListener(v -> mActivity.itemSelected(getAdapterPosition()));

            // Устанавилваем обработчик создания контекстного меню (по долгому нажатию)
            itemView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
                    menu.add(0, getAdapterPosition(), 0, "Delete");
            });

            // Устанавливаем обработчик нажатия на выключатель будильника
            alarmSwitch.setOnClickListener(v -> {
                mActivity.switchChange(getAdapterPosition());
            });
        }
    }
}
