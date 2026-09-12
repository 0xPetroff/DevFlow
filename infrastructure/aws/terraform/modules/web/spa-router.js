function handler(event) {
    var request = event.request;
    var uri = request.uri;

    // The router owns every path that is not a file, so /projects/{id} has to be served the
    // application shell rather than looked up in the bucket. Anything carrying an extension is
    // a real object and is left alone.
    if (uri.endsWith('/') || uri.lastIndexOf('.') <= uri.lastIndexOf('/')) {
        request.uri = '/index.html';
    }

    return request;
}
