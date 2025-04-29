package com.youtil.Repository;

import com.youtil.Model.Til;
import java.util.List;

public interface TilRepositoryCustom {

    List<Til> findAllByUserIdAndYear(long userId, int year);
}
