document.addEventListener('DOMContentLoaded', function() {

    let pairCount = 100; 

    // Function to add a key-value pair input
    function addKeyValuePair() {
      pairCount++;
      const pairHTML = `
        <div class="mb-3" id="pair-${pairCount}">
          <div class="input-group">
            <input type="text" class="form-control" placeholder="Key" name="key-${pairCount}" required>
            <input type="text" class="form-control" placeholder="Value" name="value-${pairCount}" required>
            <button type="button" class="btn btn-danger" onclick="removePair(${pairCount})">Remove</button>
          </div>
        </div>
      `;
      document.getElementById('keyValuePairs').insertAdjacentHTML('beforeend', pairHTML);
    }

     
    // Function to remove a key-value pair input
    window.removePair = function(id) {
      var elementId = "pair-" + id;
      const pairElement = document.getElementById(elementId);
      pairElement.remove();
    };

    // Add initial key-value pair on load
    //addKeyValuePair();

    // Event listener to add a new pair when button is clicked
    document.getElementById('addPairBtn').addEventListener('click', addKeyValuePair);

    // Form submission event handler
    document.getElementById('keyValueForm').addEventListener('submit', function(event) {
        event.preventDefault(); // Prevent default form submission

        // Collect all key-value pairs
        const formData = {};
        for (let i = 1; i <= pairCount; i++) {
          const key = document.querySelector(`input[name="key-${i}"]`);
          const value = document.querySelector(`input[name="value-${i}"]`);
          if (key && value) {
            formData[key.value] = value.value;
          }
        }

        // Send the data via Ajax
        $.ajax({
          url: '/forums/updateSeleniumTags',  // Quarkus endpoint to handle the submission
          type: 'POST',
          contentType: 'application/json',  // Indicating we're sending JSON
          data: JSON.stringify(formData),  // Convert the object to JSON
          success: function(response) {
            $('#errorMessage').empty();
            $('#successMessage').removeClass('d-none').fadeIn();  
          },
          error: function(xhr, status, error) {
            $("#successMessage").hide();     
            console.log("Error: " + error);
            $("<p>Error: " + error +"</p>").appendTo('#errorMessage');
          }
        });
      });
  });