package cz.previt.bydzovctverec.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    String error,
    List<String> errors,
    Long totalCount
) {
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, null, null, null);
  }

  public static <T> ApiResponse<T> ok(T data, long totalCount) {
    return new ApiResponse<>(true, data, null, null, totalCount);
  }

  public static <T> ApiResponse<T> error(String error) {
    return new ApiResponse<>(false, null, error, null, null);
  }

  public static <T> ApiResponse<T> error(String error, List<String> errors) {
    return new ApiResponse<>(false, null, error, errors, null);
  }
}
