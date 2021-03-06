package com.matthewsyren.runner.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.matthewsyren.runner.R;
import com.matthewsyren.runner.models.Run;
import com.matthewsyren.runner.utilities.RunInformationFormatUtilities;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RunsAdapter
        extends RecyclerView.Adapter<RunsAdapter.RunViewHolder> {
    private ArrayList<Run> mRuns;
    private IRecyclerViewOnItemClickListener mOnItemClickListener;
    private boolean mIsTwoPane;
    private int mSelectedPosition = 0;

    public RunsAdapter(ArrayList<Run> runs, IRecyclerViewOnItemClickListener onItemClickListener, boolean isTwoPane){
        mRuns = runs;
        mOnItemClickListener = onItemClickListener;
        mIsTwoPane = isTwoPane;
    }

    @NonNull
    @Override
    public RunViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.run_list_item, parent, false);
        return new RunViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RunViewHolder holder, int position) {
        Context context = holder.tvRunAverageSpeed.getContext();

        Run run = mRuns.get(position);

        //Displays the data in the appropriate Views
        holder.tvRunDate.setText(context.getString(
                R.string.run_date,
                run.getRunDate()));

        holder.tvRunDuration.setText(context.getString(
                R.string.run_duration,
                RunInformationFormatUtilities.getFormattedRunDuration(run.getRunDuration())));

        holder.tvRunDistance.setText(context.getString(
                R.string.run_distance,
                RunInformationFormatUtilities.getFormattedRunDistance(run.getDistanceTravelled(), context)));

        holder.tvRunAverageSpeed.setText(context.getString(
                R.string.run_average_speed,
                RunInformationFormatUtilities.getFormattedRunAverageSpeed(
                        run.getDistanceTravelled(),
                        run.getRunDuration(),
                        context)));

        //Displays the image for the run
        Picasso.with(context)
                .load(run.getImageUrl())
                .placeholder(R.color.colorGrey)
                .into(holder.ivRunRoute);

        //Changes the colour of the selected item when in two-pane mode
        if(mIsTwoPane){
            if(position == mSelectedPosition){
                holder.mClRunInformationListItem
                        .setBackgroundColor(context.getColor(R.color.colorAccent));
            }
            else{
                holder.mClRunInformationListItem
                        .setBackgroundColor(context.getColor(R.color.colorWhite));
            }
        }
    }

    @Override
    public int getItemCount() {
        return mRuns.size();
    }

    public void setSelectedPosition(int selectedPosition){
        mSelectedPosition = selectedPosition;
        notifyDataSetChanged();
    }

    class RunViewHolder
            extends RecyclerView.ViewHolder
            implements View.OnClickListener{
        @BindView(R.id.cl_run_information_list_item) ConstraintLayout mClRunInformationListItem;
        @BindView(R.id.tv_run_date) TextView tvRunDate;
        @BindView(R.id.tv_run_duration) TextView tvRunDuration;
        @BindView(R.id.tv_run_distance) TextView tvRunDistance;
        @BindView(R.id.tv_run_average_speed) TextView tvRunAverageSpeed;
        @BindView(R.id.iv_run_route) ImageView ivRunRoute;

        RunViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(mOnItemClickListener != null) {
                int position = getAdapterPosition();
                mOnItemClickListener.onItemClick(position);
                setSelectedPosition(position);
            }
        }
    }
}