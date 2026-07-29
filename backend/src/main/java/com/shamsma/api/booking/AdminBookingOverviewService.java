package com.shamsma.api.booking;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminBookingOverviewService {

  Page<AdminBookingOverviewRow> list(AdminBookingOverviewFilter filter, Pageable pageable);

  /** Same filter, unpaginated — for CSV export. */
  List<AdminBookingOverviewRow> listForExport(AdminBookingOverviewFilter filter);
}
