package com.youtil.Repository;

import com.youtil.Api.User.Dto.UserResponseDTO.TilListItem;
import com.youtil.Model.Til;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface TilRepositoryCustom {

    List<Til> findAllByUserIdAndYear(long userId, int year);

    List<TilListItem> findUserTils(Long userId, Pageable pageable);
}
