package com.esri.apl.ar_ocr_location.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.esri.apl.ar_ocr_location.R;
import com.esri.apl.ar_ocr_location.model.FoundCoordinate;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableList;
import androidx.recyclerview.widget.RecyclerView;

public class GeoCoordinatesListAdapter extends RecyclerView.Adapter<GeoCoordinatesListAdapter.FoundLocationHolder> {
  private ObservableList<FoundCoordinate> foundLocations;
  private RecyclerView mRecyclerView;
  private GeoCoordinateSelectionListener onGeoCoordinateSelected;

  public GeoCoordinatesListAdapter(ObservableList<FoundCoordinate> foundLocations) {
    this(foundLocations, null);
  }
  public GeoCoordinatesListAdapter(ObservableList<FoundCoordinate> foundLocations,
                                   GeoCoordinateSelectionListener onGeoCoordinateSelected) {
    this.foundLocations = foundLocations;
    this.foundLocations.addOnListChangedCallback(mListChanged);
    this.onGeoCoordinateSelected = onGeoCoordinateSelected;
  }

  @NonNull
  @Override
  public FoundLocationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.location_list_item, parent, false);
    return new FoundLocationHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull FoundLocationHolder holder, int position) {
    holder.bind(foundLocations.get(position));
  }

  @Override
  public int getItemCount() {
    return foundLocations.size();
  }

  @Override
  public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    this.mRecyclerView = recyclerView;
  }


  class FoundLocationHolder extends RecyclerView.ViewHolder {
    FoundLocationHolder(View itemView) {
      super(itemView);
      itemView.setOnClickListener(mItemClickListener);
    }

    void bind(FoundCoordinate foundLocation) {
      TextView txt = (TextView)itemView.findViewById(android.R.id.text1);
      txt.setText(foundLocation.toString());
//      txt.setSelected(foundLocation.isSelected());
    }

    private View.OnClickListener mItemClickListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int pos = getAdapterPosition();
        FoundCoordinate g = foundLocations.get(pos);
/*        for (FoundCoordinate gFl : foundLocations) {
          boolean bShouldbeSelected = g != null && gFl == g;
          if (gFl.isSelected() != bShouldbeSelected) {
            gFl.setSelected(bShouldbeSelected);
            notifyItemChanged(foundLocations.indexOf(gFl));
          }
        }*/

        if (onGeoCoordinateSelected != null) onGeoCoordinateSelected.onGeoCoordinateSelected(g);
      }
    };
  }


  private ObservableList.OnListChangedCallback mListChanged = new ObservableList.OnListChangedCallback() {
    @Override
    public void onChanged(ObservableList sender) {
      notifyDataSetChanged();
    }

    @Override
    public void onItemRangeChanged(ObservableList sender, int positionStart, int itemCount) {
      notifyItemRangeChanged(positionStart, itemCount);
    }

    @Override
    public void onItemRangeInserted(ObservableList sender, int positionStart, int itemCount) {
      notifyItemRangeInserted(positionStart, itemCount);
    }

    @Override
    public void onItemRangeMoved(ObservableList sender, int fromPosition, int toPosition, int itemCount) {
      notifyItemRangeRemoved(fromPosition, itemCount);
      notifyItemRangeInserted(toPosition, itemCount);
    }

    @Override
    public void onItemRangeRemoved(ObservableList sender, int positionStart, int itemCount) {
      notifyItemRangeRemoved(positionStart, itemCount);
    }
  };
}
