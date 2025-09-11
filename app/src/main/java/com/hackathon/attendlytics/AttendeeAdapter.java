package com.hackathon.attendlytics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.AttendeeViewHolder> {

    private List<AttendeeInfo> attendeeList = new ArrayList<>();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public static class AttendeeInfo {
        public String studentId;
        public String studentEmail;
        public Date joinTime;
        public String method;

        public AttendeeInfo(String studentId, String studentEmail, Date joinTime, String method) {
            this.studentId = studentId;
            this.studentEmail = studentEmail;
            this.joinTime = joinTime;
            this.method = method;
        }
    }

    @NonNull
    @Override
    public AttendeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendee, parent, false);
        return new AttendeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendeeViewHolder holder, int position) {
        AttendeeInfo attendee = attendeeList.get(position);
        
        // Display student email (or student ID if email not available)
        String displayName = attendee.studentEmail != null ? attendee.studentEmail : attendee.studentId;
        holder.textViewStudentName.setText(displayName);
        
        // Display join time
        String timeText = attendee.joinTime != null ? timeFormat.format(attendee.joinTime) : "Unknown";
        holder.textViewJoinTime.setText("Joined: " + timeText);
        
        // Display method
        String methodText = attendee.method != null ? attendee.method : "Unknown";
        holder.textViewMethod.setText("Via: " + methodText);
    }

    @Override
    public int getItemCount() {
        return attendeeList.size();
    }

    public void updateAttendeeList(List<AttendeeInfo> newList) {
        this.attendeeList.clear();
        this.attendeeList.addAll(newList);
        notifyDataSetChanged();
    }

    public void addAttendee(AttendeeInfo attendee) {
        this.attendeeList.add(0, attendee); // Add to top
        notifyItemInserted(0);
    }

    public int getAttendeeCount() {
        return attendeeList.size();
    }

    static class AttendeeViewHolder extends RecyclerView.ViewHolder {
        TextView textViewStudentName;
        TextView textViewJoinTime;
        TextView textViewMethod;

        public AttendeeViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewStudentName = itemView.findViewById(R.id.textViewStudentName);
            textViewJoinTime = itemView.findViewById(R.id.textViewJoinTime);
            textViewMethod = itemView.findViewById(R.id.textViewMethod);
        }
    }
}