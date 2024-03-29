package api.hackathon.iaiq.domain.question.answer.dto.request;

import api.hackathon.iaiq.global.exception.ApiException;
import api.hackathon.iaiq.global.exception.ErrorType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.Getter;

@Getter
public class SearchCondition {

    @Schema(description = "답 년도", example = "2023")
    public String year;

    @Schema(description = "답 월", example = "12")
    public String month;

    @Schema(description = "답 일", example = "25")
    public String day;

    @Schema(hidden = true)
    public LocalDate getSearchDate() {
        try {
            String dateString = String.format("%s-%s-%s", year, month, day);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(dateString, formatter);
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorType._DATE_INVALID_REQUEST);
        }
    }
}
