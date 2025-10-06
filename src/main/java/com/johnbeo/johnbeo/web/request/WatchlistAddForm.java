package com.johnbeo.johnbeo.web.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchlistAddForm {

    @NotBlank(message = "코인 ID를 입력해주세요. 예: bitcoin, ethereum")
    private String coinId;

    private String label;
}
