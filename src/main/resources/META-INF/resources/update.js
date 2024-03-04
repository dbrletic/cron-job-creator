const serialize_form = form => JSON.stringify(
    Array.from(new FormData(form).entries())
         .reduce((m, [ key, value ]) => Object.assign(m, { [key]: value }), {})
  );
  
  $('#cronForum').on('submit', function(event) {
    event.preventDefault();
    const data = {
      cronjobName: document.getElementById('cronJobName').value,
      cronJobSchedule: document.getElementById('cronJobSchedule').value
    };
    const jsonData = JSON.stringify(data);
    console.log(jsonData);
    $.ajax({
      type: 'POST',
      url: '/ffe-cronjob/update',
      data: jsonData,
      contentType: 'application/json',
      xhrFields:{
        responseType: 'blob'
      },
      success: function(response) {
        $("#cronForum")[0].reset();
        $('#errorMessage').empty();
        $("#successMessage").show();      
      },
      error: function(xhr, status, error) {
        $("#successMessage").hide();     
        console.log("Error: " + error);
        $("#cronForum")[0].reset();
        $("<p>Error: " + error +"</p>").appendTo('#errorMessage');
      }
    });
  });
  
  function fetchData() {
    var xhr = new XMLHttpRequest();
    var cronjobName = document.getElementById('verifyConJobName').value;
    var url = "/openshift/tester-pipelines/verify/" + cronjobName;
    xhr.onload = function() {
        if (xhr.status >= 200 && xhr.status < 300) {
            var response = xhr.responseText;
            document.getElementById('verifyCronJobSchedule').value = response; // Set the response in the "verifyCronJobSchedule" field
        } else {
          document.getElementById('verifyCronJobSchedule').value = "Request failed with status:" + xhr.status;
          console.error('Request failed with status: ' + xhr.status);
        }
    };

    xhr.onerror = function() {
        console.error('Request failed');
    };

    xhr.open('GET', url, true);
    xhr.send();
}

  // Add event listener to the "GetSchedule" button to trigger the AJAX call
  document.getElementById('GetSchedule').addEventListener('click', fetchData);
  
  /* Set the width of the side navigation to 250px */
  function openNav() {
    document.getElementById("mySidenav").style.width = "250px";
  }
  
  /* Set the width of the side navigation to 0 */
  function closeNav() {
    document.getElementById("mySidenav").style.width = "0";
  }
  