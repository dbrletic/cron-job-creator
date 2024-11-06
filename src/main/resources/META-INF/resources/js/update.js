const serialize_form = form => JSON.stringify(
    Array.from(new FormData(form).entries())
         .reduce((m, [ key, value ]) => Object.assign(m, { [key]: value }), {})
  );
  
  $('#cronForum').on('submit', function(event) {
    event.preventDefault();
    const data = {
      cronJobName: document.getElementById('cronJobName').value,
      cronJobSchedule: document.getElementById('cronJobSchedule').value
    };
    const jsonData = JSON.stringify(data);
    console.log(jsonData);
    $.ajax({
      type: 'POST',
      url: '/openshift/tester-pipelines/update',
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
    document.getElementById("mySidenav").style.width = "350px";
  }
  
  /* Set the width of the side navigation to 0 */
  function closeNav() {
    document.getElementById("mySidenav").style.width = "0";
  }

  function addPair() {
    const container = document.getElementById('pairContainer');
    const pairDiv = document.createElement('div');
    pairDiv.innerHTML = `
        <input type="text" name="keys" placeholder="Enter the name of the cronjob" class="form-control" required>
        <input type="text" name="values" class="form-control" placeholder="Enter schedule in * * * * * format. Use https://crontab.guru/ to verify format" required>
    `;
    container.appendChild(pairDiv);
}

async function submitPairs() {
    const keys = document.querySelectorAll('input[name="keys"]');
    const values = document.querySelectorAll('input[name="values"]');
    const pairs = {};

    for (let i = 0; i < keys.length; i++) {
        pairs[keys[i].value] = values[i].value;


    }

    const data = {
      pairs
    };
  
    $.ajax({
      type: 'POST',
      url: '/ffe-cronjob/update',
      data: JSON.stringify(data),
      contentType: 'application/json',
      xhrFields:{
        responseType: 'blob'
      },
      success: function(response) {
        $('#errorMessage').empty();
        $("#successMessage").show();    
        const container = document.getElementById('pairContainer');
        container.innerHTML = '';
        var link = document.createElement('a');
        let today = new Date().toISOString().slice(0, 10)
        link.href = window.URL.createObjectURL(response)
        link.download = "cronjob-update-" + today + ".zip";
        document.body.appendChild(link);
        link.click();
        $('#errorMessage').empty();
        $("#successMessage").show();      
      },
      error: function(xhr, status, error, textStatus) {
        $("#successMessage").hide();
        const errorContainer = document.getElementById('errorMessage');
        const container = document.getElementById('pairContainer');
        errorContainer.textContent = "Error: " + xhr.getResponseHeader("errorMsg");
        container.innerHTML = '';     
        console.log("Error: " + error);
      }
    });
}
  