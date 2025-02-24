const serialize_form = form => JSON.stringify(
    Array.from(new FormData(form).entries())
         .reduce((m, [ key, value ]) => Object.assign(m, { [key]: value }), {})
  );
  
  $('#piplelineRunForm').on('submit', function(event) {
    event.preventDefault();
    const data = {
      releaseBranch: document.getElementById('releaseBranch').value,
      userNameFFM: document.getElementById('userNameFFM').value,
      userPassword: document.getElementById('userPassword').value,
      groups: document.getElementById('groups').value,
      url: document.getElementById('url').value,
      seleniumTestEmailList: document.getElementById('seleniumTestEmailList').value,
      logs: document.getElementById('logs').value,
      mvnArgs: document.getElementById('mvnArgs').value
      //pipelineRunName: document.getElementById('pipelineRunName').value
    };
    const jsonData = JSON.stringify(data);
    $.ajax({
      type: 'POST',
      url: '/pipeline/tester-pipelines/startRun',
      data: jsonData,
      contentType: 'application/json',
      success: function(data, textStatus, jqXHR) {
        $("#piplelineRunForm")[0].reset();
        $("#successMessage").show();
        $("<p>Pipeline Run Created: " + data + "</p>").appendTo('#successMessage');
      },
      error: function(xhr, status, error) {
        $("#successMessage").hide();     
        console.log("Error: " + error);
        $("#piplelineRunForm")[0].reset();
        $("<p>Error: " + error +"</p>").appendTo('#errorMessage');
      }
    });
  });
  