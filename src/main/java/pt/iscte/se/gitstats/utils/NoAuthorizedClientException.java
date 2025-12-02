package pt.iscte.se.gitstats.utils;

public class NoAuthorizedClientException extends RuntimeException {

  public NoAuthorizedClientException(String message) {
    super(message);
  }

}
