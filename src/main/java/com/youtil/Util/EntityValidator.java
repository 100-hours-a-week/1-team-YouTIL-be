package com.youtil.Util;

import com.youtil.Common.Enums.Status;
import com.youtil.Exception.UserException.UserException.UserNotFoundException;
import com.youtil.Model.User;
import com.youtil.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntityValidator {

    private final UserRepository userRepository;


    public User getValidUserOrThrow(long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() == Status.active)
                .orElseThrow(UserNotFoundException::new);
    }

}
