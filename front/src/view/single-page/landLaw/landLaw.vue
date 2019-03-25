<template>
  <div>
    <Row>
      <Col>
        <Card>
          <Form :model="formItem" :label-width="80">
            <FormItem label="編">
              <Input v-model="law.module" placeholder="編" style="width: 300px"/>
            </FormItem>
            <FormItem label="章">
              <Input v-model="law.chapter" placeholder="章" style="width: 300px"/>
            </FormItem>
            <FormItem label="條">
              <Input v-model="law.no" placeholder="條" style="width: 300px"/>
            </FormItem>
            <FormItem label="內容">
              <Input v-model="law.content" placeholder="..." type="textarea" :rows="4"/>
            </FormItem>

            <FormItem>
              <Button type="primary" style="margin-left: 8px" @click="pre">前一條</Button>
              <Button type="primary" style="margin-left: 8px" @click="speak">朗誦</Button>
              <Button type="primary" style="margin-left: 8px" @click="next">後一條</Button>
            </FormItem>
          </Form>
        </Card>
      </Col>
    </Row>
    <Row>
      <Col>
        <Card v-if="display">
          <Table :columns="columns" :data="rows"></Table>
        </Card>
      </Col>
    </Row>
  </div>
</template>
<style scoped>
</style>
<script>
import moment from "moment";
import { getLandLaw } from "@/api/data";
export default {
  name: "landLaw",
  mounted() {
    this.fetchLaw();
  },
  data() {
    return {
      offset: 0,
      law: {
        module: "",
        chapter: "",
        no: "",
        content: "",
        terms: []
      },
      formItem: {
        monitorTypes: [],
        dateRange: "",
        start: undefined,
        end: undefined
      },
      display: false,
      columns: [],
      rows: []
    };
  },
  computed: {},
  methods: {
    fetchLaw() {
      getLandLaw({ offset: this.offset })
        .then(resp => {
          const data = resp.data;
          this.law.module = data.module;
          this.law.chapter = data.chapter;
          this.law.no = data.no;
          this.law.content = data.content;
        })
        .catch(err => {
          alert(err);
        });
    },
    pre() {
      if (this.offset) this.offset -= 1;

      this.fetchLaw();
    },
    next() {
      this.offset += 1;
      this.fetchLaw();
    },
    speak() {
      if ("speechSynthesis" in window) {
        let synth = window.speechSynthesis;
        let voices = synth.getVoices();
        let utterThis = new SpeechSynthesisUtterance(this.law.content);

        let twVoice = voices.filter(v => {
          return v.lang === "zh-TW";
        });

        utterThis.voice = twVoice[0];

        utterThis.pitch = 1;
        utterThis.rate = 1;
        synth.speak(utterThis);
      } else {
        alert("speechSynthesis is not supported!");
      }
    },
    query() {
      this.display = true;
      this.formItem.start = this.formItem.dateRange[0].getTime();
      this.formItem.end = this.formItem.dateRange[1].getTime();
      getHistoryData({
        monitorTypes: encodeURIComponent(this.formItem.monitorTypes.join(",")),
        start: this.formItem.start,
        end: this.formItem.end
      })
        .then(resp => {
          const ret = resp.data;
          this.columns.splice(0, this.columns.length);
          this.rows.splice(0, this.rows.length);
          this.columns.push({
            title: "日期",
            key: "date",
            sortable: true
          });
          for (let i = 0; i < ret.columnNames.length; i++) {
            let col = {
              title: ret.columnNames[i],
              key: `col${i}`,
              sortable: true
            };
            this.columns.push(col);
          }
          for (let row of ret.rows) {
            let rowData = {
              date: new moment(row.date).format("lll"),
              cellClassName: {}
            };
            for (let c = 0; c < row.cellData.length; c++) {
              let key = `col${c}`;
              rowData[key] = row.cellData[c].v;
              rowData.cellClassName[key] = row.cellData[c].cellClassName;
            }
            this.rows.push(rowData);
          }
        })
        .catch(err => {
          alert(err);
        });
    }
  }
};
</script>
