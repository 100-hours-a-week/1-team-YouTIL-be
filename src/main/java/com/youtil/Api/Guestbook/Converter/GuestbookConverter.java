package com.youtil.Api.Guestbook.Converter;

import com.youtil.Api.Guestbook.dto.GuestbookRequestDTO;
import com.youtil.Api.Guestbook.dto.GuestbookResponseDTO;
import com.youtil.Api.Guestbook.dto.GuestbookResponseDTO.GuestbookItem;
import com.youtil.Common.Enums.GuestbookStatus;
import com.youtil.Model.Guestbook;
import org.springframework.data.domain.Page;

import java.util.List;

public class GuestbookConverter {

    public static Guestbook toGuestbook(Long ownerId, Long guestId,
                                        GuestbookRequestDTO.CreateGuestbookRequestDTO request) {
        return Guestbook.builder()
                .ownerId(ownerId)
                .guestId(guestId)
                .content(request.getContent())
                .topGuestbookId(request.getTopGuestbookId())
                .status(GuestbookStatus.ACTIVE)
                .build();
    }

    public static GuestbookResponseDTO.CreateGuestbookResponseDTO toCreateGuestbookResponseDTO(Long guestbookId) {
        return GuestbookResponseDTO.CreateGuestbookResponseDTO.builder()
                .guestbookId(guestbookId)
                .build();
    }

    public static GuestbookResponseDTO.GetGuestbookListResponseDTO toGuestbookListResponseDTO(
            Page<GuestbookItem> guestbookPage, List<GuestbookItem> guestbooksWithReplies) {

        return GuestbookResponseDTO.GetGuestbookListResponseDTO.builder()
                .guestbooks(guestbooksWithReplies)
                .totalCount(guestbookPage.getTotalElements())
                .currentPage(guestbookPage.getNumber())
                .pageSize(guestbookPage.getSize())
                .build();
    }

    public static GuestbookItem toGuestbookItem(Guestbook guestbook) {
        return GuestbookItem.builder()
                .id(guestbook.getId())
                .guestId(guestbook.getGuestId())
                .guestNickname(guestbook.getGuest() != null ? guestbook.getGuest().getNickname() : "알 수 없는 사용자")
                .guestProfileImageUrl(guestbook.getGuest() != null ? guestbook.getGuest().getProfileImageUrl() : null)
                .content(guestbook.getContent())
                .topGuestbookId(guestbook.getTopGuestbookId())
                .createdAt(guestbook.getCreatedAt())
                .updatedAt(guestbook.getUpdatedAt())
                .deleted(guestbook.isDeleted())
                .replies(null)
                .build();
    }
    }
