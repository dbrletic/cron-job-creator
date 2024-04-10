const serialize_form = form => JSON.stringify(
    Array.from(new FormData(form).entries())
         .reduce((m, [ key, value ]) => Object.assign(m, { [key]: value }), {})
  );
  
  $('#cronForum').on('submit', function(event) {
    event.preventDefault();
    const data = {
      releaseBranch: document.getElementById('releaseBranch').value,
      url: document.getElementById('url').value,
      type: document.getElementById('type').value,
      gatlingTestEmailList: document.getElementById('gatlingTestEmailList').value,
      cronJobSchedule: document.getElementById('cronJobSchedule').value
    };
    const jsonData = JSON.stringify(data);
    console.log(jsonData);
    $.ajax({
      type: 'POST',
      url: '/ffe-cronjob/gatling',
      data: jsonData,
      contentType: 'application/json',
      xhrFields:{
        responseType: 'blob'
      },
      success: function(response) {
        $("#cronForum")[0].reset();
        var link = document.createElement('a');
        let today = new Date().toISOString().slice(0, 10)
        link.href = window.URL.createObjectURL(response)
        link.download = "gatling-" + data.url + "-" + today + ".zip";
        document.body.appendChild(link);
        link.click();
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
  
  /* Set the width of the side navigation to 250px */
  function openNav() {
    document.getElementById("mySidenav").style.width = "250px";
  }
  
  /* Set the width of the side navigation to 0 */
  function closeNav() {
    document.getElementById("mySidenav").style.width = "0";
  }
  