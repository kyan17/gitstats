package pt.iscte.se.gitstats;

public class NoAuthorizedClientException extends RuntimeException {

  public NoAuthorizedClientException(String message) {
    super(message);
  }

}
