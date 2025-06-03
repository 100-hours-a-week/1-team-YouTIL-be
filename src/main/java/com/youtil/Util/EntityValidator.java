package com.youtil.Util;

import com.youtil.Common.Enums.Status;
import com.youtil.Exception.InterviewException.InterviewException.InterviewNotFoundException;
import com.youtil.Exception.TilException.TilException.TilNotFoundException;
import com.youtil.Exception.UserException.UserException.UserNotFoundException;
import com.youtil.Model.Interview;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.InterviewRepository;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntityValidator {

    private final UserRepository userRepository;
    private final TilRepository tilRepository;
    private final InterviewRepository interviewRepository;

    public User getValidUserOrThrow(long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() == Status.active)
                .orElseThrow(UserNotFoundException::new);
    }

    public Til getValidTilOrThrow(long tilId) {
        return tilRepository.findById(tilId)
                .filter(til -> til.getStatus() == Status.active)
                .orElseThrow(TilNotFoundException::new);
    }

    public Interview getValidInterviewOrThrow(long interviewId) {
        return interviewRepository.findById(interviewId)
                .filter(interview -> interview.getStatus() == Status.active)
                .orElseThrow(InterviewNotFoundException::new);
    }
}
