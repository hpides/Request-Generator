// FiberEvaluation.cpp: Definiert den Einstiegspunkt für die Anwendung.
//

#include "FiberEvaluation.h"

using namespace std;


// Session handle.
HINTERNET hSession;

// Context value structure.
typedef struct {
    HWND        hWindow;        // Handle for the dialog box
    HINTERNET   hConnect;       // Connection handle
    HINTERNET   hRequest;       // Resource request handle
    int         nURL;           // ID of the URL edit box
    int         nHeader;        // ID of the header output box
    int         nResource;      // ID of the resource output box
    DWORD       dwSize;         // Size of the latest data block
    DWORD       dwTotalSize;    // Size of the total data
    LPSTR       lpBuffer;       // Buffer for storing read data
    WCHAR       szMemo[256];    // String providing state information
} REQUEST_CONTEXT;

static REQUEST_CONTEXT rcContext[FIBER_COUNT];
CRITICAL_SECTION g_CallBackCritSec;
WINHTTP_STATUS_CALLBACK pCallback = NULL;
atomic_int finished = 0;
// This macro returns the constant name in a string.
#define CASE_OF(constant)   case constant: return (L# constant)
#pragma comment(lib, "winhttp.lib")
LPCWSTR GetApiErrorString(DWORD dwResult)
{
	// Return the error result as a string so that the
	// name of the function causing the error can be displayed.
	switch (dwResult)
	{
		CASE_OF(API_RECEIVE_RESPONSE);
		CASE_OF(API_QUERY_DATA_AVAILABLE);
		CASE_OF(API_READ_DATA);
		CASE_OF(API_WRITE_DATA);
		CASE_OF(API_SEND_REQUEST);
	}
	return L"Unknown function";

}
void Cleanup(REQUEST_CONTEXT* cpContext)
{
	WCHAR szBuffer[256];
	// Set the memo to indicate a closed handle.
	//printf("Cleanup");


	if (cpContext->hRequest)
	{
		//printf(">WinHttpSetStatusCallback NULL");
		WinHttpSetStatusCallback(cpContext->hRequest,
			NULL,
			NULL,
			NULL);
		//printf("<WinHttpSetStatusCallback NULL");

		//printf(">WinHttpCloseHandle hRequest (%X)\n", (unsigned int)cpContext->hRequest);
		WinHttpCloseHandle(cpContext->hRequest);
		//printf("<WinHttpCloseHandle");
		cpContext->hRequest = NULL;
	}

	if (cpContext->hConnect)
	{
		//printf(">WinHttpCloseHandle hConnect (%X)\n", (unsigned int)cpContext->hConnect);
		WinHttpCloseHandle(cpContext->hConnect);
		//printf("<WinHttpCloseHandle");
		cpContext->hConnect = NULL;
	}

	delete[] cpContext->lpBuffer;
	cpContext->lpBuffer = NULL;

	// note: this function can be called concurrently by differnet threads, therefore any global data
	// reference needs to be protected

	EnterCriticalSection(&g_CallBackCritSec);
	//Re-enable the download button.
	LeaveCriticalSection(&g_CallBackCritSec);
}

// Forward declaration.
void __stdcall AsyncCallback(HINTERNET, DWORD_PTR, DWORD, LPVOID, DWORD);
BOOL SendRequest(REQUEST_CONTEXT* cpContext, LPWSTR szURL)
{
	WCHAR szHost[256];
	DWORD dwOpenRequestFlag = 0;
	URL_COMPONENTS urlComp;
	BOOL fRet = FALSE;
	WCHAR szBuffer[256];

	WINHTTP_AUTOPROXY_OPTIONS AutoProxyOptions = { 0 };
	WINHTTP_CURRENT_USER_IE_PROXY_CONFIG IEProxyConfig;
	WINHTTP_PROXY_INFO  proxyInfo = { 0 };

	// Initialize URL_COMPONENTS structure.
	ZeroMemory(&urlComp, sizeof(urlComp));
	urlComp.dwStructSize = sizeof(urlComp);

	// Use allocated buffer to store the Host Name.
	urlComp.lpszHostName = szHost;
	urlComp.dwHostNameLength = sizeof(szHost) / sizeof(szHost[0]);

	// Set non zero lengths to obtain pointer to the URL Path.
	/* note: if we threat this pointer as a NULL terminated string
			this pointer will contain Extra Info as well. */
	urlComp.dwUrlPathLength = -1;

	// Crack HTTP scheme.
	urlComp.dwSchemeLength = -1;


	//printf(">Calling WinHttpCrackURL for %s\n", szURL);

	// Crack the URL.
	if (!WinHttpCrackUrl(szURL, 0, 0, &urlComp))
	{
		//printf("< WinHttpCrackUrl failed : %X\n", GetLastError());
		goto cleanup;
	}

	// Install the status callback function.
	if (pCallback == NULL)
	{
		//printf(">Calling WinHttpSetStatusCallback with WINHTTP_CALLBACK_FLAG_ALL_NOTIFICATIONS");
		pCallback = WinHttpSetStatusCallback(hSession,
			(WINHTTP_STATUS_CALLBACK)AsyncCallback,
			WINHTTP_CALLBACK_FLAG_ALL_NOTIFICATIONS,
			NULL);
	}
	// note: On success WinHttpSetStatusCallback returns the previously defined callback function.
	// Here it should be NULL
	if (pCallback == WINHTTP_INVALID_STATUS_CALLBACK)
	{
		//printf("< WinHttpSetStatusCallback WINHTTP_INVALID_STATUS_CALLBACK");
		goto cleanup;
	}

	//printf("< WinHttpSetStatusCallback succeeded");

	//printf(">Calling WinHttpConnect for host %s and port %d\n", szHost, urlComp.nPort);
	// Open an HTTP session.
	cpContext->hConnect = WinHttpConnect(hSession, szHost,
		urlComp.nPort, 0);
	if (NULL == cpContext->hConnect)
	{
		//printf("< WinHttpConnect failed : %X\n", GetLastError());
		goto cleanup;
	}
	//printf("< WinHttpConnect  succeeded");

	//printf("> Calling WinHttpGetIEProxyConfigForCurrentUser");

	if (WinHttpGetIEProxyConfigForCurrentUser(&IEProxyConfig))
	{
		//printf("< WinHttpGetIEProxyConfigForCurrentUser succeeded");

		//
		// If IE is configured to autodetect, then we'll autodetect too
		//
		if (IEProxyConfig.fAutoDetect)
		{
			AutoProxyOptions.dwFlags = WINHTTP_AUTOPROXY_AUTO_DETECT;

			//
			// Use both DHCP and DNS-based autodetection
			//
			AutoProxyOptions.dwAutoDetectFlags = WINHTTP_AUTO_DETECT_TYPE_DHCP |
				WINHTTP_AUTO_DETECT_TYPE_DNS_A;

		}

		//
		// If there's an autoconfig URL stored in the IE proxy settings, save it
		//
		if (IEProxyConfig.lpszAutoConfigUrl)
		{
			AutoProxyOptions.dwFlags |= WINHTTP_AUTOPROXY_CONFIG_URL;
			AutoProxyOptions.lpszAutoConfigUrl = IEProxyConfig.lpszAutoConfigUrl;

		}
		//printf("> Calling WinHttpGetProxyForUrl");
		BOOL bResult = WinHttpGetProxyForUrl(hSession,
			urlComp.lpszScheme,
			&AutoProxyOptions,
			&proxyInfo);
		DWORD dwError;
		if (!bResult)
		{
			dwError = GetLastError();
			//printf("< WinHttpGetProxyForUrl failed : %X\n", dwError);
		}
		else
		{
			//printf("> Calling WinHttpSetOption");
			if (!WinHttpSetOption(hSession,
				WINHTTP_OPTION_PROXY,
				&proxyInfo,
				sizeof(proxyInfo)))
			{
				dwError = GetLastError();
				//printf("< WinHttpSetOption failed : %X\n", dwError);
			}
		}

	}
	// Prepare OpenRequest flag
	dwOpenRequestFlag = (INTERNET_SCHEME_HTTPS == urlComp.nScheme) ?
		WINHTTP_FLAG_SECURE : 0;

	//printf(">Calling WinHttpOpenRequest");

	// Open a "GET" request.
	cpContext->hRequest = WinHttpOpenRequest(cpContext->hConnect,
		L"GET", urlComp.lpszUrlPath,
		NULL, WINHTTP_NO_REFERER,
		WINHTTP_DEFAULT_ACCEPT_TYPES,
		dwOpenRequestFlag);

	if (cpContext->hRequest == 0)
	{
		//printf("< WinHttpOpenRequest failed : %X\n", GetLastError());
		goto cleanup;
	}
	//printf("< WinHttpOpenRequest succeeded");

	//printf("> Calling WinHttpSendRequest");

	// Send the request.
	if (!WinHttpSendRequest(cpContext->hRequest,
		WINHTTP_NO_ADDITIONAL_HEADERS, 0,
		WINHTTP_NO_REQUEST_DATA, 0, 0,
		(DWORD_PTR)cpContext))
	{
		//printf("< WinHttpSendRequest failed : %X\n", GetLastError());
		goto cleanup;
	}
	//printf("< WinHttpSendRequest succeeded");
	fRet = TRUE;

cleanup:

	if (fRet == FALSE)
	{
		WCHAR szError[256];

		// Set the error message.
		//printf("%s failed with error %d\n", szBuffer, GetLastError());
		// Cleanup handles.
		Cleanup(cpContext);


	}

	return fRet;
}

BOOL Header(REQUEST_CONTEXT* cpContext)
{
	DWORD dwSize = 0;
	LPVOID lpOutBuffer = NULL;
	WCHAR szBuffer[256];

	// Set the state memo.
	//printf(">Calling WinHttpQueryHeaders");

	// Use HttpQueryInfo to obtain the size of the buffer.
	if (!WinHttpQueryHeaders(cpContext->hRequest,
		WINHTTP_QUERY_RAW_HEADERS_CRLF,
		WINHTTP_HEADER_NAME_BY_INDEX, NULL, &dwSize, WINHTTP_NO_HEADER_INDEX))
	{
		// An ERROR_INSUFFICIENT_BUFFER is expected because you
		// are looking for the size of the headers.  If any other
		// error is encountered, display error information.
		DWORD dwErr = GetLastError();
		if (dwErr != ERROR_INSUFFICIENT_BUFFER)
		{

			//printf("Error %d encountered.\n", dwErr);
			return FALSE;
		}
	}

	// Allocate memory for the buffer.
	lpOutBuffer = new WCHAR[dwSize];

	// Use HttpQueryInfo to obtain the header buffer.
	if (WinHttpQueryHeaders(cpContext->hRequest,
		WINHTTP_QUERY_RAW_HEADERS_CRLF,
		WINHTTP_HEADER_NAME_BY_INDEX, lpOutBuffer, &dwSize, WINHTTP_NO_HEADER_INDEX))

	// Free the allocated memory.
	delete[] lpOutBuffer;

	return TRUE;
}


BOOL QueryData(REQUEST_CONTEXT* cpContext)
{
	WCHAR szBuffer[256];
	// Set the state memo.
	//printf(">Calling WinHttpQueryDataAvailable");

	// Chech for available data.
	if (WinHttpQueryDataAvailable(cpContext->hRequest, NULL) == FALSE)
	{
		// If a synchronous error occured, display the error.  Otherwise
		// the query is successful or asynchronous.
		DWORD dwErr = GetLastError();
		//printf("Error %d encountered.\n", dwErr);
		return FALSE;
	}
	return TRUE;
}


void TransferAndDeleteBuffers(REQUEST_CONTEXT* cpContext, LPSTR lpReadBuffer, DWORD dwBytesRead)
{
	cpContext->dwSize = dwBytesRead;

	if (!cpContext->lpBuffer)
	{
		// If there is no context buffer, start one with the read data.
		cpContext->lpBuffer = lpReadBuffer;
	}
	else
	{
		// Store the previous buffer, and create a new one big
		// enough to hold the old data and the new data.
		LPSTR lpOldBuffer = cpContext->lpBuffer;
		cpContext->lpBuffer = new char[cpContext->dwTotalSize + cpContext->dwSize];

		// Copy the old and read buffer into the new context buffer.
		memcpy(cpContext->lpBuffer, lpOldBuffer, cpContext->dwTotalSize);
		memcpy(cpContext->lpBuffer + cpContext->dwTotalSize, lpReadBuffer, cpContext->dwSize);

		// Free the memory allocated to the old and read buffers.
		delete[] lpOldBuffer;
		delete[] lpReadBuffer;
	}

	// Keep track of the total size.
	cpContext->dwTotalSize += cpContext->dwSize;
}


BOOL ReadData(REQUEST_CONTEXT* cpContext)
{
	LPSTR lpOutBuffer = new char[cpContext->dwSize + 1];
	WCHAR szBuffer[256];
	ZeroMemory(lpOutBuffer, cpContext->dwSize + 1);

	// Set the state memo.
	//printf(">Calling WinHttpReadData with size %d\n", cpContext->dwSize);

	// Read the available data.
	if (WinHttpReadData(cpContext->hRequest, (LPVOID)lpOutBuffer,
		cpContext->dwSize, NULL) == FALSE)
	{
		// If a synchronous error occurred, display the error.  Otherwise
		// the read is successful or asynchronous.
		DWORD dwErr = GetLastError();
		//printf("WinHttpReadData Error %d encountered.\n", dwErr);
		//printf("%ls\n", szBuffer);
		delete[] lpOutBuffer;
		return FALSE;
	}
	//printf("<WinHttpReadData");
	
	return TRUE;
}
//********************************************************************
//                                                   Status Callback  
//********************************************************************

void __stdcall AsyncCallback(HINTERNET hInternet, DWORD_PTR dwContext,
	DWORD dwInternetStatus,
	LPVOID lpvStatusInformation,
	DWORD dwStatusInformationLength)
{
	REQUEST_CONTEXT* cpContext;
	WCHAR szBuffer[1024];
	cpContext = (REQUEST_CONTEXT*)dwContext;
	WINHTTP_ASYNC_RESULT* pAR;

	if (cpContext == NULL)
	{
		// this should not happen, but we are being defensive here
		return;
	}

	szBuffer[0] = 0;

	// Create a string that reflects the status flag.
	switch (dwInternetStatus)
	{
	case WINHTTP_CALLBACK_STATUS_CLOSING_CONNECTION:
		//Closing the connection to the server.The lpvStatusInformation parameter is NULL.
		//printf("CLOSING_CONNECTION (%d)\n", dwStatusInformationLength);
		break;

	case WINHTTP_CALLBACK_STATUS_CONNECTED_TO_SERVER:
		//Successfully connected to the server. 
		//The lpvStatusInformation parameter contains a pointer to an LPWSTR that indicates the IP address of the server in dotted notation.
		if (lpvStatusInformation)
		{
			//printf("CONNECTED_TO_SERVER (%s)\n", (WCHAR*)lpvStatusInformation);
		}
		else
		{
			//printf("CONNECTED_TO_SERVER (%d)\n", dwStatusInformationLength);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_CONNECTING_TO_SERVER:
		//Connecting to the server.
		//The lpvStatusInformation parameter contains a pointer to an LPWSTR that indicates the IP address of the server in dotted notation.
		if (lpvStatusInformation)
		{
			//printf("CONNECTING_TO_SERVER (%s)\n", (WCHAR*)lpvStatusInformation);
		}
		else
		{
			//printf("CONNECTING_TO_SERVER (%d)\n", dwStatusInformationLength);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_CONNECTION_CLOSED:
		//Successfully closed the connection to the server. The lpvStatusInformation parameter is NULL. 
		//printf("CONNECTION_CLOSED (%d)\n", dwStatusInformationLength);
		break;

	case WINHTTP_CALLBACK_STATUS_DATA_AVAILABLE:
		//Data is available to be retrieved with WinHttpReadData.The lpvStatusInformation parameter points to a DWORD that contains the number of bytes of data available.
		//The dwStatusInformationLength parameter itself is 4 (the size of a DWORD).

		cpContext->dwSize = *((LPDWORD)lpvStatusInformation);

		// If there is no data, the process is complete.
		if (cpContext->dwSize == 0)
		{
			//printf("DATA_AVAILABLE Number of bytes available : %d. All data has been read -> Displaying the data.\n", cpContext->dwSize);
			// All of the data has been read.  Display the data.
			if (cpContext->dwTotalSize)
			{
				// Convert the final context buffer to wide characters,
				// and display.
				LPWSTR lpWideBuffer = new WCHAR[cpContext->dwTotalSize + 1];
				MultiByteToWideChar(CP_ACP, MB_PRECOMPOSED,
					cpContext->lpBuffer,
					cpContext->dwTotalSize,
					lpWideBuffer,
					cpContext->dwTotalSize);
				lpWideBuffer[cpContext->dwTotalSize] = 0;
				/* note: in the case of binary data, only data upto the first null will be displayed */
				finished++;
				//printf("%ls\n",lpWideBuffer);

				// Delete the remaining data buffers.
				delete[] lpWideBuffer;
				delete[] cpContext->lpBuffer;
				cpContext->lpBuffer = NULL;
			}

			// Close the request and connect handles for this context.
			Cleanup(cpContext);

		}
		else
		{
			//printf("DATA_AVAILABLE Number of bytes available : %d. Reading next block of data\n", cpContext->dwSize);
			// Otherwise, read the next block of data.
			if (ReadData(cpContext) == FALSE)
			{
				//printf("DATA_AVAILABLE Number of bytes available : %d. ReadData returning FALSE\n", cpContext->dwSize);
				Cleanup(cpContext);
			}
		}
		break;

	case WINHTTP_CALLBACK_STATUS_HANDLE_CREATED:
		//An HINTERNET handle has been created. The lpvStatusInformation parameter contains a pointer to the HINTERNET handle.
		if (lpvStatusInformation)
		{
			//printf("HANDLE_CREATED : %X\n", (unsigned int)lpvStatusInformation);
		}
		else
		{
			//printf("HANDLE_CREATED (%d)\n", dwStatusInformationLength);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_HANDLE_CLOSING:
		//This handle value has been terminated. The lpvStatusInformation parameter contains a pointer to the HINTERNET handle. There will be no more callbacks for this handle.
		if (lpvStatusInformation)
		{
			//printf("HANDLE_CLOSING : %X\n", (unsigned int)lpvStatusInformation);
		}
		else
		{
			//printf("HANDLE_CLOSING (%d)\n", dwStatusInformationLength);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_HEADERS_AVAILABLE:
		//The response header has been received and is available with WinHttpQueryHeaders. The lpvStatusInformation parameter is NULL.
		//printf("HEADERS_AVAILABLE (%d)\n", dwStatusInformationLength);
		Header(cpContext);

		// Initialize the buffer sizes.
		cpContext->dwSize = 0;
		cpContext->dwTotalSize = 0;

		// Begin downloading the resource.
		if (QueryData(cpContext) == FALSE)
		{
			Cleanup(cpContext);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_INTERMEDIATE_RESPONSE:
		//Received an intermediate (100 level) status code message from the server. 
		//The lpvStatusInformation parameter contains a pointer to a DWORD that indicates the status code.
		if (lpvStatusInformation)
		{
			//printf("INTERMEDIATE_RESPONSE Status code : %d\n", *(DWORD*)lpvStatusInformation);
		}
		else
		{
			//printf("INTERMEDIATE_RESPONSE (%d)\n", dwStatusInformationLength);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_NAME_RESOLVED:
		//Successfully found the IP address of the server. The lpvStatusInformation parameter contains a pointer to a LPWSTR that indicates the name that was resolved.
		if (lpvStatusInformation)
		{
			//printf("NAME_RESOLVED : %s\n", (WCHAR*)lpvStatusInformation);
		}
		else
		{
			//printf("NAME_RESOLVED (%d)\n", dwStatusInformationLength);
		}
		break;


	case WINHTTP_CALLBACK_STATUS_READ_COMPLETE:
		//Data was successfully read from the server. The lpvStatusInformation parameter contains a pointer to the buffer specified in the call to WinHttpReadData. 
		//The dwStatusInformationLength parameter contains the number of bytes read.
		//When used by WinHttpWebSocketReceive, the lpvStatusInformation parameter contains a pointer to a WINHTTP_WEB_SOCKET_STATUS structure, 
		//	and the dwStatusInformationLength parameter indicates the size of lpvStatusInformation.

		//printf("READ_COMPLETE Number of bytes read : %d\n", dwStatusInformationLength);

		// Copy the data and delete the buffers.

		if (dwStatusInformationLength != 0)
		{
			TransferAndDeleteBuffers(cpContext, (LPSTR)lpvStatusInformation, dwStatusInformationLength);

			// Check for more data.
			if (QueryData(cpContext) == FALSE)
			{
				Cleanup(cpContext);
			}
		}
		break;


	case WINHTTP_CALLBACK_STATUS_RECEIVING_RESPONSE:
		//Waiting for the server to respond to a request. The lpvStatusInformation parameter is NULL. 
		//printf("RECEIVING_RESPONSE (%d)\n", dwStatusInformationLength);
		break;

	case WINHTTP_CALLBACK_STATUS_REDIRECT:
		//An HTTP request is about to automatically redirect the request. The lpvStatusInformation parameter contains a pointer to an LPWSTR indicating the new URL.
		//At this point, the application can read any data returned by the server with the redirect response and can query the response headers. It can also cancel the operation by closing the handle

		if (lpvStatusInformation)
		{
			//printf("REDIRECT to %s\n", (WCHAR*)lpvStatusInformation);
		}
		else
		{
			//printf("REDIRECT (%d)\n", dwStatusInformationLength);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_REQUEST_ERROR:
		//An error occurred while sending an HTTP request. 
		//The lpvStatusInformation parameter contains a pointer to a WINHTTP_ASYNC_RESULT structure. Its dwResult member indicates the ID of the called function and dwError indicates the return value.
		pAR = (WINHTTP_ASYNC_RESULT*)lpvStatusInformation;
		//printf("REQUEST_ERROR - error %d, result %s\n", pAR->dwError, GetApiErrorString(pAR->dwResult));

		Cleanup(cpContext);
		break;

	case WINHTTP_CALLBACK_STATUS_REQUEST_SENT:
		//Successfully sent the information request to the server. 
		//The lpvStatusInformation parameter contains a pointer to a DWORD indicating the number of bytes sent. 
		if (lpvStatusInformation)
		{
			//printf("REQUEST_SENT Number of bytes sent : %d\n", *(DWORD*)lpvStatusInformation);
		}
		else
		{
			//printf("REQUEST_SENT (%d)\n", dwStatusInformationLength);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_RESOLVING_NAME:
		//Looking up the IP address of a server name. The lpvStatusInformation parameter contains a pointer to the server name being resolved.
		if (lpvStatusInformation)
		{
			//printf("RESOLVING_NAME %s\n", (WCHAR*)lpvStatusInformation);
		}
		else
		{
			//printf("RESOLVING_NAME (%d)\n", dwStatusInformationLength);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_RESPONSE_RECEIVED:
		//Successfully received a response from the server. 
		//The lpvStatusInformation parameter contains a pointer to a DWORD indicating the number of bytes received.
		if (lpvStatusInformation)
		{
			//printf("RESPONSE_RECEIVED. Number of bytes : %d\n", *(DWORD*)lpvStatusInformation);
		}
		else
		{
			//printf("RESPONSE_RECEIVED (%d)\n", dwStatusInformationLength);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_SECURE_FAILURE:
		//One or more errors were encountered while retrieving a Secure Sockets Layer (SSL) certificate from the server. 
		/*If the dwInternetStatus parameter is WINHTTP_CALLBACK_STATUS_SECURE_FAILURE, this parameter can be a bitwise-OR combination of one or more of the following values:
			WINHTTP_CALLBACK_STATUS_FLAG_CERT_REV_FAILED
			Certification revocation checking has been enabled, but the revocation check failed to verify whether a certificate has been revoked.The server used to check for revocation might be unreachable.
			WINHTTP_CALLBACK_STATUS_FLAG_INVALID_CERT
			SSL certificate is invalid.
			WINHTTP_CALLBACK_STATUS_FLAG_CERT_REVOKED
			SSL certificate was revoked.
			WINHTTP_CALLBACK_STATUS_FLAG_INVALID_CA
			The function is unfamiliar with the Certificate Authority that generated the server's certificate.
			WINHTTP_CALLBACK_STATUS_FLAG_CERT_CN_INVALID
			SSL certificate common name(host name field) is incorrect, for example, if you entered www.microsoft.com and the common name on the certificate says www.msn.com.
			WINHTTP_CALLBACK_STATUS_FLAG_CERT_DATE_INVALID
			SSL certificate date that was received from the server is bad.The certificate is expired.
			WINHTTP_CALLBACK_STATUS_FLAG_SECURITY_CHANNEL_ERROR
			The application experienced an internal error loading the SSL libraries.
		*/
		if (lpvStatusInformation)
		{
			//printf("SECURE_FAILURE (%d).\n", *(DWORD*)lpvStatusInformation);
			if (*(DWORD*)lpvStatusInformation & WINHTTP_CALLBACK_STATUS_FLAG_CERT_REV_FAILED)  //1
			{
				wcscat_s(szBuffer, L"Revocation check failed to verify whether a certificate has been revoked.");
			}
			if (*(DWORD*)lpvStatusInformation & WINHTTP_CALLBACK_STATUS_FLAG_INVALID_CERT)  //2
			{
				wcscat_s(szBuffer, L"SSL certificate is invalid.");
			}
			if (*(DWORD*)lpvStatusInformation & WINHTTP_CALLBACK_STATUS_FLAG_CERT_REVOKED)  //4
			{
				wcscat_s(szBuffer, L"SSL certificate was revoked.");
			}
			if (*(DWORD*)lpvStatusInformation & WINHTTP_CALLBACK_STATUS_FLAG_INVALID_CA)  //8
			{
				wcscat_s(szBuffer, L"The function is unfamiliar with the Certificate Authority that generated the server\'s certificate.");
			}
			if (*(DWORD*)lpvStatusInformation & WINHTTP_CALLBACK_STATUS_FLAG_CERT_CN_INVALID)  //10
			{
				wcscat_s(szBuffer, L"SSL certificate common name(host name field) is incorrect");
			}
			if (*(DWORD*)lpvStatusInformation & WINHTTP_CALLBACK_STATUS_FLAG_CERT_DATE_INVALID)  //20
			{
				wcscat_s(szBuffer, L"CSSL certificate date that was received from the server is bad.The certificate is expired.");
			}
			if (*(DWORD*)lpvStatusInformation & WINHTTP_CALLBACK_STATUS_FLAG_SECURITY_CHANNEL_ERROR)  //80000000
			{
				wcscat_s(szBuffer, L"The application experienced an internal error loading the SSL libraries.");
			}
		}
		else
		{
			//printf("SECURE_FAILURE (%d)\n", dwStatusInformationLength);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_SENDING_REQUEST:
		// Sending the information request to the server.The lpvStatusInformation parameter is NULL.
		//printf("SENDING_REQUEST (%d)\n", dwStatusInformationLength);
		break;

	case WINHTTP_CALLBACK_STATUS_SENDREQUEST_COMPLETE:
		//printf("SENDREQUEST_COMPLETE (%d)\n", dwStatusInformationLength);

		// Prepare the request handle to receive a response.
		if (WinHttpReceiveResponse(cpContext->hRequest, NULL) == FALSE)
		{
			Cleanup(cpContext);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_WRITE_COMPLETE:
		//Data was successfully written to the server. The lpvStatusInformation parameter contains a pointer to a DWORD that indicates the number of bytes written.
		//When used by WinHttpWebSocketSend, the lpvStatusInformation parameter contains a pointer to a WINHTTP_WEB_SOCKET_STATUS structure, 
		//and the dwStatusInformationLength parameter indicates the size of lpvStatusInformation.
		if (lpvStatusInformation)
		{
			//printf("WRITE_COMPLETE (%d)\n", *(DWORD*)lpvStatusInformation);
		}
		else
		{
			//printf("WRITE_COMPLETE (%d)\n", dwStatusInformationLength);
		}
		break;

	case WINHTTP_CALLBACK_STATUS_GETPROXYFORURL_COMPLETE:
		// The operation initiated by a call to WinHttpGetProxyForUrlEx is complete. Data is available to be retrieved with WinHttpReadData.
		//printf("GETPROXYFORURL_COMPLETE (%d)\n", dwStatusInformationLength);
		break;

	case WINHTTP_CALLBACK_STATUS_CLOSE_COMPLETE:
		// The connection was successfully closed via a call to WinHttpWebSocketClose.
		//printf("CLOSE_COMPLETE (%d)\n", dwStatusInformationLength);
		break;

	case WINHTTP_CALLBACK_STATUS_SHUTDOWN_COMPLETE:
		// The connection was successfully shut down via a call to WinHttpWebSocketShutdown
		//printf("SHUTDOWN_COMPLETE (%d)\n", dwStatusInformationLength);
		break;

	default:
		//printf("Unknown/unhandled callback - status %d given\n", dwInternetStatus);
		break;
	}
}
LPVOID g_lpFiber[FIBER_COUNT];
VOID
__stdcall sendHttpRequest(LPVOID lpParameter)
{
	int id = (int)lpParameter;
	//printf("Fiber %d is running!\n", id);
	if (hSession != NULL)
	{
		rcContext[id].hWindow = NULL;
		rcContext[id].nURL = IDC_URL1;
		rcContext[id].nHeader = IDC_HEADER1;
		rcContext[id].nResource = IDC_RESOURCE1;
		rcContext[id].hConnect = 0;
		rcContext[id].hRequest = 0;
		rcContext[id].lpBuffer = NULL;
		rcContext[id].szMemo[0] = 0;
		SendRequest(&rcContext[id], L"http://192.168.137.1");
	}
	else
	{
		fprintf(stderr, "hSession is null!");
	}
	if (id < FIBER_COUNT - 1) {
		SwitchToFiber(g_lpFiber[id + 1]);
	}
	SwitchToFiber(g_lpFiber[PRIMARY_FIBER]);
}

int main()
{
	InitializeCriticalSection(&g_CallBackCritSec);
	cout << "Hello CMake." << endl;
    hSession = WinHttpOpen(L"Asynchronous WinHTTP Demo/1.0",
        WINHTTP_ACCESS_TYPE_DEFAULT_PROXY,
        WINHTTP_NO_PROXY_NAME,
        WINHTTP_NO_PROXY_BYPASS,
        WINHTTP_FLAG_ASYNC);
	g_lpFiber[PRIMARY_FIBER] = ConvertThreadToFiber(NULL);

	if (g_lpFiber[PRIMARY_FIBER] == NULL)
	{
		//printf("ConvertThreadToFiber error (%d)\n", GetLastError());
		return 1;
	}
	time_t start, end, duration;
	start = time(0);
	for (int i = PRIMARY_FIBER + 1; i < FIBER_COUNT; i++) {
		g_lpFiber[i] = CreateFiber(0, sendHttpRequest, (LPVOID) i);
		if(g_lpFiber[i] == NULL) {
			//printf("CreateFiber error (%d)\n", GetLastError());
			return 1;
		}
	}
	
	
	end = time(0);
	duration = end - start;

	printf("\n\n %ld fibers creation took %llu seconds\n", FIBER_COUNT, duration);
	start = time(0);
	SwitchToFiber(g_lpFiber[1]);
	printf("Back to primary fiber\n");
	while (finished < FIBER_COUNT - 1) {
		printf("%ld remaining.\n", FIBER_COUNT - 1 - finished);
		Sleep(1000);
	}
	end = time(0);
	duration = end - start;

	printf("\n\n %ld fibers duration %llu seconds\n", FIBER_COUNT, duration);
	// Close the session handle.
	WinHttpCloseHandle(hSession);
	return 0;
}