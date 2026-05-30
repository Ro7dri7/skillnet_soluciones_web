package com.skillnet.persistence.repository.projection;

import java.time.LocalDate;

public interface DailyCountProjection {

    LocalDate getDate();

    Long getCount();
}
