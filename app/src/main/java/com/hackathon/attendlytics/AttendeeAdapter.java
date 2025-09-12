package com.hackathon.attendlytics;

import android.util.Log;
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

    private static final String TAG = "AttendeeAdapter";
    private List<AttendeeInfo> attendeeList = new ArrayList<>();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public static class AttendeeInfo {
        public String studentId;
        public String studentEmail;
        public String studentName;
        public String rollNumber;
        public Date joinTime;
        public String method;

        public AttendeeInfo(String studentId, String studentEmail, String studentName, String rollNumber, Date joinTime, String method) {
            this.studentId = studentId;
            this.studentEmail = studentEmail;
            this.studentName = studentName;
            this.rollNumber = rollNumber;
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
        
        // Debug logging to see what data is being displayed
        Log.d(TAG, "Displaying attendee at position " + position + 
                  " - studentName: '" + attendee.studentName + "'" + 
                  ", rollNumber: '" + attendee.rollNumber + "'" + 
                  ", studentEmail: '" + attendee.studentEmail + "'");
        
        // Build display text prioritizing roll number visibility
        String displayText = "";
        
        // Check if we have a valid roll number
        if (attendee.rollNumber != null && !attendee.rollNumber.isEmpty() && 
            !attendee.rollNumber.equals("Not Set") && !attendee.rollNumber.equals("Not Available") && 
            !attendee.rollNumber.equals("Not Registered")) {
            
            // We have a valid roll number - display it prominently
            displayText = "🎓 " + attendee.rollNumber;
            
            // Add student name if available and different from email
            if (attendee.studentName != null && !attendee.studentName.isEmpty() && 
                !attendee.studentName.equals(attendee.studentEmail)) {
                displayText += " (" + attendee.studentName + ")";
            }
        } else {
            // No valid roll number available
            if (attendee.studentName != null && !attendee.studentName.isEmpty() && 
                !attendee.studentName.equals(attendee.studentEmail)) {
                displayText = "👤 " + attendee.studentName + " (⚠️ No Roll Number)";
            } else {
                // No name either, show email with warning
                String email = attendee.studentEmail != null ? attendee.studentEmail : attendee.studentId;
                displayText = "📧 " + email + " (⚠️ Profile Incomplete)";
            }
        }
        
        Log.d(TAG, "Final display text: '" + displayText + "'");
        
        holder.textViewStudentName.setText(displayText);
        
        // Display join time
        String timeText = attendee.joinTime != null ? timeFormat.format(attendee.joinTime) : "Unknown";
        holder.textViewJoinTime.setText("⏰ Joined: " + timeText);
        
        // Display method with icon
        String methodText = attendee.method != null ? attendee.method : "Unknown";
        String methodIcon = "📱";
        if ("QR Code".equals(methodText)) {
            methodIcon = "📷";
        } else if ("BLE".equals(methodText)) {
            methodIcon = "📶";
        }
        holder.textViewMethod.setText(methodIcon + " Via: " + methodText);
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