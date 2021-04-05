# play-monad

This play module provides a simpler alternative to play Actions using for-comprehensions.

NOTE: We just started work on this repository, expect many changes.

## Preview

Write simple and reusable Play Action's using for-comprehensions.

```scala
def saveAction = MonadicAction {
  for {
    auth <- header("Authorization")
    user <- authService.authorize(auth)
    jsonBody <- body(BodyParsers.parse.tolerantJson)
  } yield {
    // your business logic here
  }
}
```

Each step in the for-comprehension might fail. Just as a `Future` carries the concept of a hidden `Throwable`, this Monad carries a hidde failure `Result` response from Play. If the header is missing a `BadRequest` will be returned, if the authService doesn't authorize the request, a `Forbidden` will be returned and errors processing the body would return other error codes as well.

## Motivation

### Simpler and more composable request processing

### Validating headers before processing the body

The original spark for the idea came to be when using Play Framework for a project where requests included huge file uploads.
These uploads could be in the hundreds of MB or even a few GBs. Uploading took a long time. It was possible headers were
invalid, user was not authenticated and other validations could go wrong. This could lead to a lot of wasted time when headers
where invalid. HTTP supports the `100-continue` feature. It allows users to send the headers first, wait for confirmation
these are accepted and that it may continue to send the body.

Play supports the `100-continue` feature in principle but when using Play's Actions with body parsers, the body is
processed fully before the headers. To leverage this feature a different way of constructing Actions is needed.

Finally, to make sure every endpoint supports this, it is imperative all header processing happens before using the body.
To enable that it was needed to make the Monad stateful. Headers cannot be accessed after processing the body and this
is enforced by the compiler.

## Acknowledgements

The idea for this library was conceived by @rcano around 2012-2014 and was implemented in a closed project.
Since then, this pattern has been re-implemented in other places by Tomás Lázaro (@tlazaro) in a different shape but same
spirit. After many years seeing Play Actions has not changed and no implementation exists in the wild we decided to
do this work in open-source and share it.

The [play-monadic-actions](https://github.com/Kanaka-io/play-monadic-actions) shares the same core idea and has a variety
of conversions that are very useful. However, it does not include the concept of a stateful Monad and does not enable
usage of the 100-continue HTTP feature.
