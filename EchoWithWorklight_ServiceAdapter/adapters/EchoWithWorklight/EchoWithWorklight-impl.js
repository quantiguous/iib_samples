
function prepareRequest(input, path, oneway) {
    var request = {
        method : 'post',
        path : path,
        body : {
            contentType: 'application/json; charset=utf-8',
            content: input
        }
    };

	if (oneway == false) {
		request['returnedContentType'] = 'json';
	}    
    return request;
}

function sendRequest(request) {
    WL.Logger.log(request);
    var response = WL.Server.invokeHttp(request);
    WL.Logger.log(response);
    return response;
}

function echo(input) {
    var request = prepareRequest(input, '/Worklight/EchoWithWorklight/echo', false);
    return sendRequest(request);
}

